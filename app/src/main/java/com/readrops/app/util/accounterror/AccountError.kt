package com.readrops.app.util.accounterror

import android.content.Context
import com.readrops.api.utils.exceptions.HttpException
import com.readrops.api.utils.exceptions.LoginFailedException
import com.readrops.api.utils.exceptions.ParseException
import com.readrops.api.utils.exceptions.UnknownFormatException
import com.readrops.app.R
import com.readrops.app.repositories.FeedExistException
import com.readrops.app.util.diagnostics.nonStandardPort
import com.readrops.app.util.diagnostics.readableDescription
import com.readrops.app.util.diagnostics.readableMessage
import com.readrops.app.util.diagnostics.suggestsBlockedConnection
import com.readrops.db.entities.account.Account
import com.readrops.db.entities.account.AccountType
import java.io.IOException
import java.io.InterruptedIOException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

abstract class AccountError(
    protected val context: Context,
    /** Server of the account, used to name a port the current network may be blocking. */
    private val serverUrl: String? = null,
) {

    open fun newFeedMessage(exception: Exception): String = genericMessage(exception)

    open fun updateFeedMessage(exception: Exception): String = genericMessage(exception)

    open fun deleteFeedMessage(exception: Exception): String = genericMessage(exception)

    open fun newFolderMessage(exception: Exception): String = genericMessage(exception)

    open fun updateFolderMessage(exception: Exception): String = genericMessage(exception)

    open fun deleteFolderMessage(exception: Exception): String = genericMessage(exception)

    /**
     * @param url server or feed that failed, when it differs from the account server, as for
     * the feeds of a local account or an address being typed at login
     */
    fun genericMessage(exception: Exception, url: String? = serverUrl) = when {
        exception is HttpException -> httpMessage(exception)
        exception is UnknownHostException -> context.resources.getString(R.string.unreachable_url)

        // must stay above IOException, which SSLException inherits from. Self hosted setups
        // very often use a self signed certificate, and the raw handshake message says
        // nothing useful. The app already trusts user added authorities, so the advice works.
        exception is SSLException -> context.resources.getString(R.string.tls_error)
        exception is NoSuchFileException -> context.resources.getString(R.string.unable_open_file)

        // above IOException too: a timeout or refused connection is what a network blocking
        // the server port produces, and its raw message was often empty, shown as "null"
        exception.suggestsBlockedConnection() -> blockedConnectionMessage(exception, url)
        exception is IOException -> context.resources.getString(
            R.string.network_failure,
            exception.readableMessage()
        )

        exception is ParseException || exception is UnknownFormatException ->
            context.resources.getString(R.string.processing_feed_error)
        exception is LoginFailedException -> context.getString(R.string.login_failed)
        exception is FeedExistException -> context.getString(R.string.feed_already_exists)
        else -> context.getString(R.string.unexpected_error, exception.readableDescription())
    }

    private fun blockedConnectionMessage(exception: Exception, url: String?): String {
        val failure = if (exception is InterruptedIOException) {
            context.getString(R.string.network_timeout)
        } else {
            context.getString(R.string.server_unreachable)
        }

        val hint = nonStandardPort(url)
            ?.let { context.getString(R.string.blocked_port_hint, it) }
            ?: context.getString(R.string.blocked_network_hint)

        return "$failure $hint"
    }

    protected fun httpMessage(exception: HttpException): String {
        return when (exception.code) {
            in 400..499 -> {
                when (exception.code) {
                    400 -> context.resources.getString(R.string.http_error_400)
                    401 -> context.resources.getString(R.string.http_error_401)
                    403 -> context.resources.getString(R.string.http_error_403)
                    404 -> context.resources.getString(R.string.http_error_404)
                    else -> context.resources.getString(R.string.http_error_4XX, exception.code)
                }
            }

            in 500..599 -> {
                context.resources.getString(R.string.http_error_5XX, exception.code)
            }

            else -> context.resources.getString(R.string.http_error, exception.code)
        }
    }

    companion object {

        fun from(account: Account, context: Context): AccountError = when (account.type) {
            AccountType.FRESHRSS, AccountType.GREADER -> GReaderError(context, account.url)
            AccountType.NEXTCLOUD_NEWS -> NextcloudNewsError(context, account.url)
            else -> DefaultAccountError(context, account.url)
        }

        class DefaultAccountError(context: Context, serverUrl: String? = null) :
            AccountError(context, serverUrl)
    }
}

