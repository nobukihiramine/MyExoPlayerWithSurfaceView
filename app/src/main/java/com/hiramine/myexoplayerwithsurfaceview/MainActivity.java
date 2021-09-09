package com.hiramine.myexoplayerwithsurfaceview;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;

import java.util.Locale;

public class MainActivity extends AppCompatActivity
{
	private final String m_strUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
	// 再生動画として、commondatastorage.googleapis.com の gtv-videos-bucket の sample に置いてある「Big Buck Bunny」動画ファイルを使用。
	// Big Buck Bunny
	// 　Copyright (C) 2008 Blender Foundation | peach.blender.org Some Rights Reserved.
	// 　Creative Commons Attribution 3.0 license. https://peach.blender.org/

	private SurfaceView     m_surfaceviewVideo;
	private SimpleExoPlayer m_simpleexoplayer;
	private boolean         m_bIsPlayingWhenActivityIsPaused;
	private TextView        m_textviewPosition;
	private TextView        m_textviewDuration;
	private SeekBar         m_seekbar;
	private Handler         m_handlerUpdateProgress = new Handler( Looper.getMainLooper() );    // メインスレッド（＝UIスレッド）で実行させるHandler。
	private boolean         m_bNowSeekBarTouching;
	private ViewGroup       m_viewgroupPlayerController;
	private ImageView       m_imageviewPlay;
	private ImageView       m_imageviewPause;
	private ImageView       m_imageviewRew10;
	private ImageView       m_imageviewFF10;
	private ViewGroup		m_viewgropuVideo;
	private ImageView       m_imageviewFullScreen;
	private boolean         m_bIsFullScreen;

	private View.OnLayoutChangeListener m_onVideoViewGroupLayoutChangeListener = new View.OnLayoutChangeListener()
	{
		@Override
		public void onLayoutChange( View view,
									int left, int top, int right, int bottom,
									int oldLeft, int oldTop, int oldRight, int oldBottom )
		{
			if( left != oldLeft
				|| top != oldTop
				|| right != oldRight
				|| bottom != oldBottom )
			{    // ビューのサイズが変わった
				adjustVideoViewSize();
			}
		}
	};

	private View.OnClickListener m_onFullScreenButtonClickListener = new View.OnClickListener()
	{
		@Override
		public void onClick( View view )
		{
			if( m_bIsFullScreen )
			{    // フルスクリーン中。非フルスクリーンへ移行。画面の向きは、縦向き。
				m_imageviewFullScreen.setImageDrawable( ResourcesCompat.getDrawable( getResources(), R.drawable.ic_fullscreen, null ) );
				setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_PORTRAIT );
				m_bIsFullScreen = false;
			}
			else
			{    // 非フルスクリーン中。フルスクリーンへ移行。画面の向きは、横向き。
				m_imageviewFullScreen.setImageDrawable( ResourcesCompat.getDrawable( getResources(), R.drawable.ic_fullscreen_exit, null ) );
				setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE );
				m_bIsFullScreen = true;
			}
		}
	};

	private View.OnClickListener m_onPlayerControllerClickListener = new View.OnClickListener()
	{
		@Override
		public void onClick( View view )
		{
			if( m_imageviewPlay == view )
			{
				m_simpleexoplayer.play();
			}
			else if( m_imageviewPause == view )
			{
				m_simpleexoplayer.pause();
			}
			else if( m_imageviewRew10 == view )
			{
				long lPos = m_simpleexoplayer.getCurrentPosition();
				lPos -= 10 * 1000;
				m_simpleexoplayer.seekTo( lPos );
			}
			else if( m_imageviewFF10 == view )
			{
				long lPos = m_simpleexoplayer.getCurrentPosition();
				lPos += 10 * 1000;
				m_simpleexoplayer.seekTo( lPos );
			}
		}
	};

	private SeekBar.OnSeekBarChangeListener m_onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener()
	{
		@Override
		public void onProgressChanged( SeekBar seekBar, int i, boolean b )
		{

		}

		@Override
		public void onStartTrackingTouch( SeekBar seekBar )
		{
			m_bNowSeekBarTouching = true;
		}

		@Override
		public void onStopTrackingTouch( SeekBar seekBar )
		{
			long lPositionMilliSec = seekBar.getProgress() * 100;
			m_simpleexoplayer.seekTo( lPositionMilliSec );

			m_bNowSeekBarTouching = false;
		}
	};

	private Runnable m_runnableUpdateProgress = new Runnable()
	{
		@Override
		public void run()
		{
			updateProgress();

			m_handlerUpdateProgress.postDelayed( this, 100 );            // 100ミリ秒（0.1秒）後に再度実行する
		}
	};

	private final Player.Listener m_playerlistener = new Player.Listener()
	{
		@Override
		public void onIsPlayingChanged( boolean isPlaying )
		{
			if( isPlaying )
			{
				// 動画を再生するビューのサイズの調整
				adjustVideoViewSize();

				// 動画長さの設定
				// 動画の長さは、exoplayer.getDuration()によって、単位がミリ秒で得られる。
				long lDurationMilliSec = m_simpleexoplayer.getDuration();
				// 動画の長さのテキストは、動画の長さを「MM:SS」形式で設定する。
				long lDurationSec = (int)Math.ceil( lDurationMilliSec / 1000 );
				long lMM          = lDurationSec / 60;
				long lSS          = lDurationSec % 60;
				m_textviewDuration.setText( String.format( Locale.US, "%02d:%02d", lMM, lSS ) );
				// シークバーの最大値は、動画の長さを単位を0.1秒で設定する（100で割って、0.1秒単位に変換）。
				m_seekbar.setMax( (int)( lDurationMilliSec / 100 ) );
			}

			// 「再生」「一時停止」ボタンの更新
			updatePlayPauseButton();
		}
	};

	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_main );

		// 初期状態は、画面は縦向きで、非フルスクリーン。
		setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_PORTRAIT );
		m_bIsFullScreen = false;

		// Viewの取得
		m_textviewPosition = findViewById( R.id.textview_position );
		m_textviewDuration = findViewById( R.id.textview_duration );
		m_seekbar = findViewById( R.id.seekbar );
		m_viewgroupPlayerController = findViewById( R.id.viewgroup_playercontroller );
		m_imageviewPlay = findViewById( R.id.imageview_play );
		m_imageviewPause = findViewById( R.id.imageview_pause );
		m_imageviewRew10 = findViewById( R.id.imageview_rew_10 );
		m_imageviewFF10 = findViewById( R.id.imageview_ff_10 );
		m_viewgropuVideo = findViewById( R.id.viewgroup_video );
		m_imageviewFullScreen = findViewById( R.id.imageview_fullscreen );

		// リスナーの設定
		m_seekbar.setOnSeekBarChangeListener( m_onSeekBarChangeListener );
		m_imageviewPlay.setOnClickListener( m_onPlayerControllerClickListener );
		m_imageviewPause.setOnClickListener( m_onPlayerControllerClickListener );
		m_imageviewRew10.setOnClickListener( m_onPlayerControllerClickListener );
		m_imageviewFF10.setOnClickListener( m_onPlayerControllerClickListener );
		m_viewgropuVideo.addOnLayoutChangeListener( m_onVideoViewGroupLayoutChangeListener );
		m_imageviewFullScreen.setOnClickListener( m_onFullScreenButtonClickListener );

		// 動画を再生するビューの取得
		m_surfaceviewVideo = findViewById( R.id.surfaceview );

		// SimpleExoPlayerの作成と設定
		SimpleExoPlayer.Builder playerbuilder = new SimpleExoPlayer.Builder( this );
		m_simpleexoplayer = playerbuilder.build();
		m_simpleexoplayer.addListener( m_playerlistener );

		// 動画を再生するビューとSimpleExoPlayerの紐づけ
		m_simpleexoplayer.setVideoSurfaceView( m_surfaceviewVideo );

		// MediaItemの作成とSimpleExoPlayerへのセット
		Uri uriVideo = Uri.parse( m_strUri );
		m_simpleexoplayer.setMediaItem( MediaItem.fromUri( uriVideo ) );

		// 再生の準備
		m_simpleexoplayer.prepare();
		// 再生の開始
		m_simpleexoplayer.play();
	}

	// 初回表示時、および、ポーズからの復帰時
	@Override
	protected void onResume()
	{
		super.onResume();

		// タイトルバー、ステータスバー、ナビゲーションバーの非表示
		// （ポーズ、ストップから復帰した際には、ナビゲーションバーのImmersiveモード（ナビゲーションバーの折り畳み表示）が解除されるので、
		// 　onCreateではなく、onResumeで、バー非表示処理を行う。
		// 　onCreateで、ウインドウのフルスクリーン設定を行うと、ポーズからレジュームした際に、ナビゲーションバーが常時表示となってしまう。）
		getWindow().getDecorView().setSystemUiVisibility( View.SYSTEM_UI_FLAG_LAYOUT_STABLE // タイトルバー非表示
														  | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN // ステータスバーが無いものとしてビューをレイアウトする
														  | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION // ナビゲーションバーが無いものとしてビューをレイアウトする
														  | View.SYSTEM_UI_FLAG_FULLSCREEN // ステータスバーの非表示
														  | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // ナビゲーションバーの非表示
														  | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY );// ナビゲーションバーの折り畳み表示ねばねばモード（スワイプで表示。しばらくすると再非表示）

		// 再生位置表示の定期更新の開始
		m_handlerUpdateProgress.postDelayed( m_runnableUpdateProgress, 0 );

		if( m_bIsPlayingWhenActivityIsPaused )
		{    // Activity pause時に動画再生中なら、動画再生再開
			m_simpleexoplayer.play();
		}
	}

	// アクティビティがフォアグラウンドからバックグラウンドに追いやられた時
	@Override
	protected void onPause()
	{
		// 動画再生中かどうかの取得と保持
		m_bIsPlayingWhenActivityIsPaused = m_simpleexoplayer.isPlaying();
		// 動画再生の一時停止
		m_simpleexoplayer.pause();

		// 再生位置表示の定期更新の停止
		m_handlerUpdateProgress.removeCallbacks( m_runnableUpdateProgress );

		super.onPause();
	}

	@Override
	public boolean onTouchEvent( MotionEvent event )
	{
		super.onTouchEvent( event );

		switch( event.getAction() )
		{
			case MotionEvent.ACTION_DOWN:
				int iVisibility = m_viewgroupPlayerController.getVisibility();
				if( View.VISIBLE == iVisibility )
				{    // Visible ⇒ Invisible
					m_viewgroupPlayerController.setVisibility( View.INVISIBLE );
				}
				else
				{    // Invisible ⇒ Visible
					m_viewgroupPlayerController.setVisibility( View.VISIBLE );
				}
				return true;
		}

		return false;
	}

	// 動画を再生するビューのサイズの調整
	private void adjustVideoViewSize()
	{
		// 動画があるかの確認
		Format videoformat = m_simpleexoplayer.getVideoFormat();
		if( null == videoformat )
		{
			return;
		}

		// 動画のサイズ
		int iVideoWidth  = videoformat.width;
		int iVideoHeight = videoformat.height;

		// 動画を再生するビューの親のサイズ
		ViewGroup viewgroupVideo = (ViewGroup)m_surfaceviewVideo.getParent();
		int iViewgroupWidth  = viewgroupVideo.getWidth();
		int iViewgroupHeight = viewgroupVideo.getHeight();

		// 動画の縦横比を保持しつつ、動画を再生するビューの親の中で最大限引きのばした、幅と高さを求める
		ViewGroup.LayoutParams layoutparams = m_surfaceviewVideo.getLayoutParams();
		double dVideoRatio     = (float)iVideoWidth / iVideoHeight;
		double dViewgropuRatio = (float)iViewgroupWidth / iViewgroupHeight;
		if( dVideoRatio > dViewgropuRatio )
		{
			layoutparams.width = iViewgroupWidth;
			layoutparams.height = (int)( (double)layoutparams.width / dVideoRatio );
		}
		else
		{
			layoutparams.height = iViewgroupHeight;
			layoutparams.width = (int)( (double)layoutparams.height * dVideoRatio );
		}

		// 求めた幅と高さを、動画を再生するビューに設定
		m_surfaceviewVideo.setLayoutParams( layoutparams );
	}

	// 再生位置表示の更新
	private void updateProgress()
	{
		// 動画の再生位置は、exoplayer.getCurrentPosition()によって、単位がミリ秒で得られる。
		long lPositionMilliSec = m_simpleexoplayer.getCurrentPosition();

		// 動画の再生位置のテキストは、動画の再生位置を「MM:SS」形式で設定する。
		long lPositionSec = lPositionMilliSec / 1000;
		long lMM          = lPositionSec / 60;
		long lSS          = lPositionSec % 60;
		m_textviewPosition.setText( String.format( Locale.US, "%02d:%02d", lMM, lSS ) );

		if( !m_bNowSeekBarTouching)
		{	// シークバーをタッチしていないときのみ処理する
			// プログレス位置は、動画の再生位置を単位を0.1秒で設定する（100で割って、0.1秒単位に変換）。
			m_seekbar.setProgress( (int)( lPositionMilliSec / 100 ) );
			return;
		}
	}

	// 「再生」「一時停止」ボタンの更新
	private void updatePlayPauseButton()
	{
		if( m_simpleexoplayer.isPlaying() )
		{    // 再生中(STATE_READY = state & true == playWhenReady)
			// ⇒ 「Play」非表示、「Pause」表示
			m_imageviewPlay.setVisibility( View.GONE );
			m_imageviewPause.setVisibility( View.VISIBLE );
		}
		else
		{    // 一時停止中(STATE_READY = state & false == playWhenReady)か、
			// 最後まで再生して停止中(STATE_ENDED == state)か、
			// 再生抑制中(player.getPlaybackSuppressionReason())か、
			// バッファリング中(STATE_ENDED = state)か、
			// 再生するメディアが無い(STATE_IDLE)か、
			// ⇒ 「Play」表示、「Pause」非表示
			m_imageviewPlay.setVisibility( View.VISIBLE );
			m_imageviewPause.setVisibility( View.GONE );
		}
	}
}