package org.shu.plug.timedautostart;

import java.util.HashMap;
import com.biglybt.pif.Plugin;
import com.biglybt.pif.PluginException;
import com.biglybt.pif.PluginInterface;
import com.biglybt.pif.download.Download;
import com.biglybt.pif.logging.LoggerChannel;
import com.biglybt.pif.ui.*;
import com.biglybt.pif.ui.menus.MenuItem;
import com.biglybt.pif.ui.menus.MenuItemFillListener;
import com.biglybt.pif.ui.menus.MenuItemListener;
import com.biglybt.pif.ui.menus.MenuManager;
import com.biglybt.pif.ui.tables.TableManager;
import com.biglybt.pif.ui.tables.TableRow;
import com.biglybt.pif.utils.LocaleUtilities;
import com.biglybt.ui.swt.pif.UISWTInputReceiver;
import com.biglybt.ui.swt.pif.UISWTInstance;

public class Main implements Plugin {
	
	private PluginInterface pluginInterface;
	private MenuManager menuManager;
	private LoggerChannel logger;
	private UISWTInstance uISWTInstance;
	private static HashMap<Integer, AutoStartThread> hAutoStartThread = new HashMap<Integer, AutoStartThread>();

	@Override
	public void initialize(PluginInterface pluginInterface) throws PluginException {
		this.pluginInterface = pluginInterface;
		this.pluginInterface.getUtilities().getLocaleUtilities().integrateLocalisedMessageBundle("AutoStartMessage");
		
		logger = pluginInterface.getLogger().getChannel("TimedAutoStart");
		menuManager = pluginInterface.getUIManager().getMenuManager();
		
        this.pluginInterface.getUIManager().addUIListener(new UIManagerListener() {

			@Override
			public void UIAttached(UIInstance instance) {
            	
                if (instance instanceof UISWTInstance) {
                	uISWTInstance = ((UISWTInstance)instance);
                    createSysTrayMenuItem();
                    createAutoStartSeletedStoppedTorrentsTableMenuItem();
                }
            }

			@Override
            public void UIDetached(UIInstance instance) {
            	if (instance instanceof UISWTInstance){
                    
            	}
            }
        });
        
	}
	
	public static boolean clearSession(int hashCode){
		if (hAutoStartThread.containsKey(hashCode)){
    		hAutoStartThread.remove(hashCode);
    		return true;
    	}
    	return false;
	}
	
	
	protected void createAutoStartSeletedStoppedTorrentsTableMenuItem() {
		MenuItem tableMenuItemAutoStart = pluginInterface.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE, "shu.plugin.tablemenu" );
		MenuItem tableMenuItemCancelAutoStart = pluginInterface.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE, "shu.plugin.tablemenu.cancel" );
		
		MenuItem tableMenuItemAutoStartSeed = pluginInterface.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_MYTORRENTS_COMPLETE, "shu.plugin.tablemenu" );
		MenuItem tableMenuItemCancelAutoStartSeed = pluginInterface.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_MYTORRENTS_COMPLETE, "shu.plugin.tablemenu.cancel" );
		
		
		MenuItemFillListener cancelFillListener = new MenuItemFillListener(){

			@Override
			public void menuWillBeShown(MenuItem menu, Object data) {
				TableRow[] rows = ((TableRow[])data);
				
				if ( rows == null || rows.length <= 0 ){
		    		return;
		    	}
				Download[] downloads = toDownloadArray(rows);
		    	showCancelMenuAndTimeLeft(menu, downloads, "shu.plugin.tablemenu");
			}
		};
		
		MenuItemListener cancelMultiListener = new MenuItemListener(){

			@Override
			public void selected(MenuItem menu, Object target) {
				TableRow[] rows = ((TableRow[])target);
				
				if ( rows == null || rows.length <= 0 ){
		    		return;
		    	}
				Download[] downloads = toDownloadArray(rows);
				cancelAutoStart(downloads, "shu.plugin.tablemenu");
				
			}
		};
		
		/*MenuItemFillListener startFillListener = new MenuItemFillListener(){

			@Override
			public void menuWillBeShown(MenuItem menu, Object data) {
				TableRow[] rows = ((TableRow[])data);
				
				if ( rows == null || rows.length <= 0 ){
		    		return;
		    	}
				Download[] downloads = toDownloadArray(rows);
				
		    	if(hasAllTimers(downloads)){
		    		menu.setVisible(false);
		    	} else {
		    		menu.setVisible(true);
		    	}
			}
			
		};*/
		MenuItemListener startMultiListener = new MenuItemListener(){

			@Override
			public void selected(MenuItem menu, Object target) {
				TableRow[] rows = ((TableRow[])target);
				
				if ( rows == null || rows.length <= 0 ){
		    		return;
		    	}
				Download[] downloads = toDownloadArray(rows);
				autoStartTorrents(downloads, "shu.plugin.tablemenu");
			}
			
		};
		
		tableMenuItemCancelAutoStart.addFillListener(cancelFillListener);
		tableMenuItemCancelAutoStart.addMultiListener(cancelMultiListener);
		//tableMenuItemAutoStart.addFillListener(startFillListener);
		tableMenuItemAutoStart.addMultiListener(startMultiListener);
		
		tableMenuItemCancelAutoStartSeed.addFillListener(cancelFillListener);
		tableMenuItemCancelAutoStartSeed.addMultiListener(cancelMultiListener);
		//tableMenuItemAutoStartSeed.addFillListener(startFillListener);
		tableMenuItemAutoStartSeed.addMultiListener(startMultiListener);
	}
	
	
	protected void showCancelMenuAndTimeLeft(MenuItem menu, Download[] downloads, String ressourceKey) {
		if(hasAtLeastOneTimer(downloads)){
    		String hashCodesKey = calculateHashCodesKey(downloads);
    		String remainingTime = "";
    		AutoStartThread autoStartThread;
    		for(int i = 0; i < downloads.length; i++){
    			
    			if (hAutoStartThread.containsKey(downloads[i].hashCode())){
    				
    				autoStartThread = hAutoStartThread.get(downloads[i].hashCode());
    				remainingTime = pluginInterface.getUtilities().getFormatters().formatTimeFromSeconds(autoStartThread.getTimeRemaining());
    				if (downloads.length == 1) break;
    				if ( !(autoStartThread.getHashCodesKey().equals(hashCodesKey)) ){
    					remainingTime = "Unknown";
    					break;
    				}
    			}
    		}
    		menu.setText(pluginInterface.getUtilities().getLocaleUtilities().getLocalisedMessageText(ressourceKey+".cancel") + " (" + remainingTime + ")");
    		menu.setVisible(true);
    	} else {
    		menu.setVisible(false);
    	}
	}
	
	protected Download[] toDownloadArray(TableRow[] rows) {
		Download[] downloads = new Download[rows.length];
		for(int i = 0; i < downloads.length; i++){
			downloads[i] = (Download)rows[i].getDataSource();
		}
		return downloads;
	}
	
	public void ipc_cancelAutoStart(Download[] downloads) {
		cancelAutoStart(downloads, "shu.plugin.tablemenu");
	}
	
	protected void cancelAutoStart(Download[] downloads, String ressourceKey) {
		AutoStartThread autoStartingThread;
    	for(int i=0 ; i<downloads.length; i++){
    		
    		if(hAutoStartThread.containsKey(downloads[i].hashCode())){
    			autoStartingThread = hAutoStartThread.get(downloads[i].hashCode());
    			autoStartingThread.interrupt();
    			hAutoStartThread.remove(downloads[i].hashCode());
    		}
    	}
		LocaleUtilities local = pluginInterface.getUtilities().getLocaleUtilities();
		logger.logAlert(LoggerChannel.LT_INFORMATION, local.getLocalisedMessageText(ressourceKey+".cancelled"));
	}
	
	public void ipc_autoStartTorrents(Download[] downloads) {
		autoStartTorrents(downloads, "shu.plugin.tablemenu");
	}

	protected void autoStartTorrents(Download[] downloads, String ressourceKey) {
		try {
			final boolean cancelTimer = hasAtLeastOneTimer(downloads)
					? askYesNoQuestion(ressourceKey, ".canceltimer") : false;
			final boolean autorestartStoppedTorrents = hasAtLeastOneStoppedTorrent(downloads)
					? askYesNoQuestion(ressourceKey,".autorestartstoppedtorrentmessage")
					: false;

			askNumberOfSecondsToWait(ressourceKey, new AskNumSecondsResults() {
				@Override
				public void numberOfSecondsToWait(long ms) {
					autoStartTorrents2(downloads, cancelTimer,
							autorestartStoppedTorrents, ms);
				}

				@Override
				public void cancelled() {

				}
			});

		} catch (CancelException e1) {
			return;
		}
	}

	protected void autoStartTorrents2(Download[] downloads,  boolean cancelTimer,
	                                  boolean autorestartStoppedTorrents, long ms) {
		AutoStartThread autoStartingThread, autoStartingThreadNew;
		
    	for(int i=0 ; i<downloads.length; i++){
    		if(hAutoStartThread.containsKey(downloads[i].hashCode())){
    			if (!cancelTimer) continue;
    			autoStartingThread = hAutoStartThread.get(downloads[i].hashCode());
    			
    			synchronized (autoStartingThread) {
    				autoStartingThread.interrupt();
    				hAutoStartThread.remove(downloads[i].hashCode());
    				try {
            			autoStartingThread.join();
    				} catch (InterruptedException e) {
    					logger.log("Thread was interrupted so we don't have to join", e);
    				}
				}
    		}
			autoStartingThreadNew = new AutoStartThread(logger, downloads[i], ms, autorestartStoppedTorrents, calculateHashCodesKey(downloads));
    		hAutoStartThread.put(downloads[i].hashCode(), autoStartingThreadNew);
    		autoStartingThreadNew.start();
    	}
	}
	
	protected boolean hasAtLeastOneStoppedTorrent(Download[] downloads) {
		for(int i=0 ; i<downloads.length; i++){
    		if(downloads[i].getState() == Download.ST_STOPPED ){
    			return true;
    		}
    	}
		return false;
	}

	protected boolean hasAtLeastOneTimer(Download[] downloads) {
		for(int i=0 ; i<downloads.length; i++){
    		if(hAutoStartThread.containsKey( downloads[i].hashCode() )){
    			return true;
    		}
    	}
		return false;
	}
	
	protected boolean hasAllTimers(Download[] downloads) {
		for(int i=0 ; i<downloads.length; i++){
    		if(!hAutoStartThread.containsKey( downloads[i].hashCode() )){
    			return false;
    		}
    	}
		return true;
	}

	protected boolean askYesNoQuestion(String ressourceKey, String subKey) throws CancelException {//TODO Yes/no buttons
		UIMessage uiMessage = uISWTInstance.createMessage();
		uiMessage.setMessage(ressourceKey+""+subKey);
		uiMessage.setTitle(ressourceKey+""+subKey+".title");
		uiMessage.setInputType(UIMessage.INPUT_YES_NO_CANCEL);
		uiMessage.setMessageType(UIMessage.MSG_QUESTION);
		
		int result = uiMessage.ask();
		if (result == UIMessage.ANSWER_CANCEL) throw new CancelException();
		return UIMessage.ANSWER_YES == result;
	}

	private interface AskNumSecondsResults {
		void numberOfSecondsToWait(long ms);

		void cancelled();
	}
	
	protected long askNumberOfSecondsToWait(String ressourceKey, final AskNumSecondsResults l) {
		UISWTInputReceiver inputReceiver = (UISWTInputReceiver) uISWTInstance.getInputReceiver();
		inputReceiver.setMessage(ressourceKey+".inputmessage");
		inputReceiver.setTitle(ressourceKey+".inputmessage.title");
		inputReceiver.setPreenteredText("5", true);
		inputReceiver.allowEmptyInput(false);
		inputReceiver.setInputValidator(new NumberValidator());

		inputReceiver.prompt(new UIInputReceiverListener() {
			@Override
			public void UIInputReceiverClosed(UIInputReceiver receiver) {

				if (receiver.hasSubmittedInput()) {
					l.numberOfSecondsToWait(((long) (Double.valueOf(receiver.getSubmittedInput()) * 60 * 1000)));
				} else {
					l.cancelled();
				}
			}
		});
		return ((long)(Double.valueOf(inputReceiver.getSubmittedInput()) * 60 * 1000));
	}
	
	protected void createSysTrayMenuItem() {
		MenuItem menuItemAutoStartTray = menuManager.addMenuItem(MenuManager.MENU_SYSTRAY, "shu.plugin.systray");
		menuItemAutoStartTray.setDisposeWithUIDetach(UIInstance.UIT_SWT);
		MenuItem menuItemCancelAutoStartTray = menuManager.addMenuItem(MenuManager.MENU_SYSTRAY, "shu.plugin.systray.cancel");
		menuItemCancelAutoStartTray.setDisposeWithUIDetach(UIInstance.UIT_SWT);


		menuItemCancelAutoStartTray.addFillListener(new MenuItemFillListener(){

			@Override
			public void menuWillBeShown(MenuItem menu, Object data) {
				showCancelMenuAndTimeLeft(menu, pluginInterface.getDownloadManager().getDownloads(), "shu.plugin.systray");
			}
        	
        });
		
		menuItemCancelAutoStartTray.addListener(new MenuItemListener(){

			@Override
			public void selected(MenuItem menu, Object target) {
				cancelAutoStart(pluginInterface.getDownloadManager().getDownloads(), "shu.plugin.systray");
			}
        	
        });
		
		menuItemAutoStartTray.addFillListener(new MenuItemFillListener(){

			@Override
			public void menuWillBeShown(MenuItem menu, Object data) {
				if(hasAllTimers(pluginInterface.getDownloadManager().getDownloads())){
		    		menu.setVisible(false);
		    	} else {
		    		menu.setVisible(true);
		    	}
			}
        	
        });
		
		menuItemAutoStartTray.addListener(new MenuItemListener(){

			@Override
			public void selected(MenuItem menu, Object target) {
				Download[] downloads = pluginInterface.getDownloadManager().getDownloads();
				autoStartTorrents(downloads, "shu.plugin.systray");
			}
        	
        });
	}

	protected String calculateHashCodesKey(Download[] downloads) {
		StringBuilder hashCodesKey = new StringBuilder();
		for(int i = 0; i < downloads.length; i++){
			hashCodesKey.append(downloads[i].hashCode()+",");
		}
		if (hashCodesKey.length() > 0) hashCodesKey.deleteCharAt(hashCodesKey.length()-1);
		return hashCodesKey.toString();
	}
	
}
