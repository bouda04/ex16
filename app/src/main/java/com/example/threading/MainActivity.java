package com.example.threading;

import java.lang.Thread.State;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;
 
public class MainActivity extends Activity {
	static MyLongTask3 myAsynch=null;
	static Thread myThread1=null;
	static Thread myThread2=null;
	

	Handler messageHandler=null;
	private final int WAIT=1; 
	private final int DURATION=10000;
	private final int HANDLER_MSG_PROGRESS = 1;
	private final int HANDLER_MSG_SOF = 2;
	private Integer sharedCounter=0;
	private boolean go = true;
	private Object goLock = new Object();
	private Object syncher = new Object();
	private int trigger = 0;
	private final int DELAY = 5000;
	
	@Override
	protected void onResume() {

		new Updater((TextView)findViewById(R.id.tvCountShared)).start();
		super.onResume();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		messageHandler = new Handler() {
			@Override
	        public void handleMessage(Message msg) {
	        	
	        	super.handleMessage(msg);
	        	TextView tv = (TextView)findViewById(R.id.tvCountRun);
	        	if (msg.what == HANDLER_MSG_PROGRESS){
	        		((TextView)findViewById(msg.arg2)).setText((Integer.toString(msg.arg1)));
	        	}
	        	else if (msg.what == HANDLER_MSG_SOF)
	        		((ToggleButton)findViewById(msg.arg2)).setChecked(false);
	        }
	    };

		((ToggleButton)findViewById(R.id.tbRun)).
			setOnCheckedChangeListener(new OnCheckedChangeListener() {			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				MyLongTask myTask=null;
				if (isChecked){	
					if (myThread1 == null || !myThread1.isAlive()){
						myTask = new MyLongTask(messageHandler, DURATION, R.id.tvCountRun);
						myThread1 = new Thread(myTask);						
						myThread1.start();
					}
					else myTask.changeHandler(messageHandler);
				}  
				else if (myThread1 != null){
					myThread1.interrupt();	
					myThread1 = null;
				}
			}
		}); 
		
		((ToggleButton)findViewById(R.id.tbAsynch)).
		setOnCheckedChangeListener(new OnCheckedChangeListener() {			
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			MyLongTask myTask=null;
			if (isChecked){	
				if (myThread2 == null || !myThread2.isAlive()){
					myTask = new MyLongTask(messageHandler, DURATION, R.id.tvCountAsynch);
					myThread2 = new Thread(myTask);						
					myThread2.start();
				}
				else myTask.changeHandler(messageHandler);
			}  
			else if (myThread2 != null){
				myThread2.interrupt();	
				myThread2 = null;
			}
		}
	}); 
		
	/*	comment out just to demonstrate the use of AsynchTask 
		((ToggleButton)findViewById(R.id.tbAsynch)).
		setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked){	
					myAsynch = new MyLongTask3();
					myAsynch.execute(DURATION);
				}
				else if (myAsynch != null){
					myAsynch.cancel(true);
					myAsynch=null;
				}
			}
		});
		*/
	}

	//first try without 'synchronized' to show that the sharedCounter 
	//doesn't return to zero
	private  void updateShared(final boolean up){
		synchronized(syncher) {
			int temp = this.sharedCounter;
			temp = temp + (up ? 1 : -1);
			this.sharedCounter = temp;
		}
	}
	
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// runnable that uses handler to communicate back to the UI THREAD	
	private class MyLongTask implements Runnable{
		Handler handler;
		int untilVal;
		int viewID;

		public MyLongTask(Handler handler, int untilVal, int viewID){
			this.handler = handler;
			this.untilVal= untilVal;
			this.viewID = viewID;
		}
		
		public void changeHandler(Handler newHandler){
			this.handler = newHandler;
		}
		@Override
		public void run() { 
			try {
				Message msg;
				synchronized (goLock) {
					while (!go) goLock.wait();
					go=false;
				}
				for (int i=1;i<this.untilVal;i++){
					if (i >= trigger){
						synchronized (goLock) {
							go = true;
							goLock.notifyAll();							
						}
					}
					Thread.sleep(WAIT); 
					updateShared(this.viewID== R.id.tvCountRun);
					msg = new Message();
					msg.what = HANDLER_MSG_PROGRESS;
					msg.arg1 = i; 
					msg.arg2 = this.viewID;
					handler.sendMessage(msg);	
				}
				msg = new Message();
				msg.what = HANDLER_MSG_SOF;
				msg.arg2 = this.viewID== R.id.tvCountRun? R.id.tbRun:R.id.tbAsynch;
				handler.sendMessage(msg);
			} catch (Exception e) {
				return;
			}
		}
	}
	
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

		private class Updater extends Thread{
			TextView view;
			
			public Updater(TextView v){
				this.view = v;
			}
			
			@Override
			public void run() {
				for (;;){
					view.post(new Runnable() {
						@Override
						public void run() { 
							view.setText((Integer.toString(sharedCounter))); 
						}
					});
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace(); 
					}
				}

			}	
	}
		
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		
		private  class MyLongTask3 extends AsyncTask<Integer, Integer, Integer>{
			TextView view;
			

			@Override
			protected void onPreExecute (){
				((TextView)findViewById(R.id.tvCountAsynch)).setText("0");
			}

			@Override
			protected Integer doInBackground(Integer... params) {
				int i=1;
				synchronized (goLock) {
					while (!go)
						try {
							goLock.wait();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					go=false;
				} 
				try {
					for (;i<params[0];i++){
						if (i >= trigger){
							synchronized (goLock) {
								go = true;
								goLock.notifyAll();							
							}
						}
						Thread.sleep(WAIT);
						updateShared(false);
						publishProgress(i);		
					}
				} catch (Exception e) {
					return i; 
				}
				 
				return i;
			}
			
			@Override
			protected void onProgressUpdate (Integer... values){
				((TextView)findViewById(R.id.tvCountAsynch)).setText(Integer.toString(values[0]));
			}
			
			@Override
			protected void onPostExecute (Integer result){
        		((ToggleButton)findViewById(R.id.tbAsynch)).setChecked(false);
			}
			
			
		
	}
		
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		
		private class MyLongTask4 extends Thread{
			
			int untilVal;
			Handler handler;
			public MyLongTask4(Handler handler, int untilVal){
				this.handler = handler;
				this.untilVal= untilVal;
			}
			@Override
			public void run() {
				try {
					for (int i=0;i<this.untilVal;i++){
							Thread.sleep(100);
							Message msg = new Message();
							msg.what = HANDLER_MSG_PROGRESS;
							msg.arg1 = i;
							handler.sendMessage(msg);
							
					}
					Message msg = new Message();
					msg.what = HANDLER_MSG_SOF;
					handler.sendMessage(msg);
				} catch (InterruptedException e) {
					return;
				}
				
			}
			
			public void changeHandler(Handler newHandler){
				this.handler = newHandler;
			}
		
	}
		
		public void resetAll(View v){
			if (myThread1 != null){
				myThread1.interrupt();	
				myThread1 = null;
			}
			
			if (myAsynch != null){
				myAsynch.cancel(true);
				myAsynch=null;
			}
			((ToggleButton)findViewById(R.id.tbAsynch)).setChecked(false);
			((ToggleButton)findViewById(R.id.tbRun)).setChecked(false);
			((TextView)findViewById(R.id.tvCountAsynch)).setText(Integer.toString(0));
			((TextView)findViewById(R.id.tvCountRun)).setText(Integer.toString(0));
			((TextView)findViewById(R.id.tvCountShared)).setText(Integer.toString(0));
			this.sharedCounter = 0;
		
		}
		
		public void startThem(View v){
			resetAll(null);
			synchronized (goLock) {
				go = false;
				trigger= (v.getId()==R.id.btBoth?0:DELAY);
				((ToggleButton)findViewById(R.id.tbAsynch)).setChecked(true);
				((ToggleButton)findViewById(R.id.tbRun)).setChecked(true);
				go = true;
				goLock.notifyAll();
			}
		}
		
		
}
