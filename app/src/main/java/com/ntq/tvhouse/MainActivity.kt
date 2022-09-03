package com.ntq.tvhouse

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
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
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.ui.StyledPlayerView.SHOW_BUFFERING_WHEN_PLAYING
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.ntq.tvhouse.entities.Channel
import com.ntq.tvhouse.ui.theme.TVHouseTheme
import com.skydoves.landscapist.coil.CoilImage
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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

    override fun onPause() {
        super.onPause()
        finishAffinity()
    }
}

@Composable
fun HomeScreen(onNavigateToFriends: (channel:Channel) -> Unit) {
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val channelFile = File(LocalContext.current.filesDir.absolutePath, "channel_config.json")
    try {
        if (channelFile.exists()){
            channelFile.delete()
        }
        val latch = CountDownLatch(1)
        Thread{
            channelFile.writeBytes(URL("https://raw.githubusercontent.com/thienquang199x/tvhouse/main/channel_config.json").openStream().readBytes())
            latch.countDown()
        }.start()
        latch.await(1, TimeUnit.MINUTES)
    } catch (t:Throwable){

    }
    val type =
        Types.newParameterizedType(List::class.java, Channel::class.java)
    val adapter = moshi.adapter<List<Channel>>(type)
    val channels = arrayListOf<Channel>()
    if (channelFile.exists()){
        channels.addAll(adapter.fromJson(channelFile.readText()) ?: arrayListOf())
    } else {
        channels.addAll(adapter.fromJson(String(LocalContext.current.assets.open("channel_config.json").readBytes()))  ?: arrayListOf())
    }

    Box {
        Image(painter = painterResource(id = R.drawable.img_bg), contentDescription = null, contentScale = ContentScale.FillBounds)
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Kênh truyền hình", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                userScrollEnabled = true
            ){
                items(channels){ channel ->
                    var padding by remember { mutableStateOf(0.dp) }
                    Card(modifier = Modifier
                        .width(200.dp)
                        .aspectRatio(728f / 409)
                        .background(color = Color.Transparent)
                        .padding(padding)
                        .onFocusChanged {
                            padding = if (it.isFocused) 0.dp else 10.dp
                        }
                        .clickable {
                            onNavigateToFriends(channel)
                        },
                        shape = RoundedCornerShape(10.dp)) {
                        CoilImage(imageModel = channel.icon, contentScale = ContentScale.Crop, failure = {
                            Text(
                                text = channel.name, color = Color.Gray, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, modifier = Modifier.align(Alignment.Center)
                            )
                        })
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
                    setShowBuffering(SHOW_BUFFERING_WHEN_PLAYING)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                }
            })
        }
    }
}