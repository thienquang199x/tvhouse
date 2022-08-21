package com.ntq.tvhouse

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.ntq.tvhouse.entities.Channel
import com.ntq.tvhouse.ui.theme.TVHouseTheme


class MainActivity : ComponentActivity() {
    private lateinit var navController:NavHostController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            navController = rememberNavController()
            TVHouseTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    NavHost(navController = navController, startDestination = "home"){
                        composable("home"){
                            HomeScreen(onNavigateToFriends = { channel -> navController.navigate("player/?channelUrl=${channel.url}")})
                        }
                        composable("player/?channelUrl={channelUrl}"){
                            PlayerScreen(channelUrl = it.arguments?.getString("channelUrl")!!)
                        }
                    }
                }
            }
        }

        hideSystemUI()
    }

    private fun hideSystemUI() {

        //Hides the ugly action bar at the top
        actionBar?.hide()

        //Hide the status bars

        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            window.insetsController?.apply {
                hide(WindowInsets.Type.statusBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }
}

@Composable
fun HomeScreen(onNavigateToFriends: (channel:Channel) -> Unit) {
    val channels = listOf(Channel("HTV2", R.drawable.img_htv2, "https://drm-livecdn.hplus.com.vn/CDN-FPT02/HTV2-HD-1080p/playlist.m3u8"), Channel("HTV3", R.drawable.img_htv3, "https://livecdn.fptplay.net/sdb/htv3_2000.stream/chunklist.m3u8"), Channel("HTV4", R.drawable.img_htv4, "https://livecdn.fptplay.net/sdb/htv4_hls.smil/chunklist_b2500000.m3u8"))
    Box() {
        Image(painter = painterResource(id = R.drawable.img_bg), contentDescription = null, contentScale = ContentScale.FillWidth)
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Kênh truyền hình", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ){
                items(channels){ channel ->
                    Card(modifier = Modifier
                        .width(100.dp)
                        .aspectRatio(728f / 409)
                        .background(color = Color.White)
                        .clickable {
                            onNavigateToFriends(channel)
                        }, elevation = 14.dp) {
                        Image(painter = painterResource(id = channel.icon), contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerScreen(channelUrl:String){
    val context = LocalContext.current
    val mExoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            val dataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
            val hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(Uri.parse(channelUrl)))
            setMediaSource(hlsMediaSource)
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(Unit){
        onDispose {
            mExoPlayer.stop()
            mExoPlayer.release()
        }
    }
    Surface(
        color = Color.Black
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(modifier = Modifier.align(Alignment.Center),factory = { context ->
                StyledPlayerView(context).apply {
                    player = mExoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                }
            })
        }
    }
}