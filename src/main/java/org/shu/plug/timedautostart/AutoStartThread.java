package org.shu.plug.timedautostart;

import java.util.GregorianCalendar;
import com.biglybt.pif.download.Download;
import com.biglybt.pif.download.DownloadException;
import com.biglybt.pif.logging.LoggerChannel;

public class AutoStartThread extends Thread {
	
	private LoggerChannel logger;
	private Download download;
	private long seconds;
	private boolean autorestartStoppedTorrents;
	private GregorianCalendar sleepDate = new GregorianCalendar();
	private String hashCodesKey;
	private boolean stopped;
	
	public AutoStartThread(LoggerChannel logger, Download download, long seconds, boolean autorestartStoppedTorrents, String hashCodesKey ){
		this.logger = logger;
		this.download = download;
		this.seconds = seconds;
		this.autorestartStoppedTorrents = autorestartStoppedTorrents;
		this.hashCodesKey = hashCodesKey;
		this.stopped = download.getState() == Download.ST_STOPPED;
	}

	@Override
	public void run(){
		try {
			if (stopped){
				if (autorestartStoppedTorrents){
					try {
						download.restart();
					} catch (DownloadException e) {
						this.logger.logAlert("Error while restarting the download", e);
						e.printStackTrace();
					}
				} else {
					return ;
				}
			}
			
			download.pause();
			sleepDate = new GregorianCalendar();
			Thread.sleep(seconds);
			
		} catch (InterruptedException e) {
			this.logger.log("Sleep was interrupted : auto start torrent cancelled", e);
			if (stopped){
				try {
					download.resume();
					download.stop();
					download.pause();
				} catch (DownloadException e1) {
					this.logger.logAlert("Error while stopping the download", e1);
					e1.printStackTrace();
				}
			}
		} finally{
			download.resume();
			clearMemory();
		}
	}

	private void clearMemory() {
    	if (!Main.clearSession(download.hashCode())){
    		logger.log(LoggerChannel.LT_ERROR, "Security messure : ClearSession returned false ... hashMap has already been cleaned up");
    	}
	}
	
	public long getTimeRemaining(){
		return Math.abs(seconds/1000 - ( (System.currentTimeMillis() - this.sleepDate.getTimeInMillis()) / 1000 ));
	}

	public String getHashCodesKey() {
		return hashCodesKey;
	}
}
