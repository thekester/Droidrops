package com.readrops.app.feeds.newfeed

/** A small offline directory of well known feeds. Search and category filtering work offline. */
data class FeedDirectoryItem(
    val title: String,
    val publisher: String,
    val url: String,
    val category: String,
    val description: String
)

object FeedDirectory {
    val categories = listOf("All", "Technology", "Open source", "Science", "News")

    val items = listOf(
        FeedDirectoryItem("Android Developers Blog", "Android Developers", "https://developer.android.com/static/blog/atom.xml", "Technology", "Android development news and guidance"),
        FeedDirectoryItem("F-Droid News", "F-Droid", "https://f-droid.org/feed", "Open source", "News from the free Android app repository"),
        FeedDirectoryItem("F-Droid Forum", "F-Droid", "https://forum.f-droid.org/latest.rss", "Open source", "Latest discussions from the F-Droid community"),
        FeedDirectoryItem("GitLab Blog", "GitLab", "https://about.gitlab.com/atom.xml", "Open source", "Software development and open source"),
        FeedDirectoryItem("LWN.net", "LWN.net", "https://lwn.net/headlines/rss", "Open source", "Linux and free software news"),
        FeedDirectoryItem("The Register", "The Register", "https://www.theregister.com/headlines.atom", "Technology", "Technology industry news"),
        FeedDirectoryItem("Ars Technica", "Ars Technica", "https://feeds.arstechnica.com/arstechnica/index", "Technology", "Technology, science and policy"),
        FeedDirectoryItem("NASA News", "NASA", "https://www.nasa.gov/feed/", "Science", "Space exploration and research"),
        FeedDirectoryItem("NASA Earth Observatory", "NASA", "https://earthobservatory.nasa.gov/feeds/image-of-the-day.rss", "Science", "Earth science images and stories"),
        FeedDirectoryItem("BBC World News", "BBC", "https://feeds.bbci.co.uk/news/world/rss.xml", "News", "International news from the BBC"),
        FeedDirectoryItem("The Guardian World", "The Guardian", "https://www.theguardian.com/world/rss", "News", "Global news and analysis"),
        FeedDirectoryItem("The Guardian Technology", "The Guardian", "https://www.theguardian.com/technology/rss", "Technology", "Technology news and analysis")
    )
}
