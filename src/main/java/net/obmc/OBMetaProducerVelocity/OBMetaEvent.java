package net.obmc.OBMetaProducerVelocity;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;

import net.kyori.adventure.text.Component;

public class OBMetaEvent {

	private final Logger logger;
	private final ProxyServer server;
	private final String program;

	private final List<String> autoDisconnect = new ArrayList<>();

	public OBMetaEvent() {
		logger = OBMetaProducer.instance.getLogger();
		server = OBMetaProducer.instance.getServer();
		program = OBMetaProducer.instance.getProgram();

		autoDisconnect.add("MCList");
	}
	
	@Subscribe
    public void onPreLogin(PreLoginEvent event) {
        InboundConnection connection = event.getConnection();
        SocketAddress ipAddress = connection.getRemoteAddress();
        logger.info("Connection coming from " + ipAddress.toString() + " for " + event.getUsername() + ", ver: " + connection.getProtocolVersion());
        if ( autoDisconnect.contains(event.getUsername())) {
        	logger.info("Disconnected " + event.getUsername());
			event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text("You are not authorized to connect to ob-mc.net")));
        }
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
    	// send message to all players that someone has joined OB
    	for (Player player : server.getAllPlayers()) {
    		if (!event.getPlayer().getUsername().equals(player.getUsername())) {
    			player.sendMessage(Component.text(event.getPlayer().getUsername() + " has joined the network."));
    		}
        }
    }
    
    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
    	
    	ServerConnection eventServer = event.getPlayer().getCurrentServer().get();
   		String disconnectMsg = "PlayerDisconnectEvent#" + event.getPlayer().getUsername() + "#ob-" + eventServer.getServerInfo().getName() + "#" + getTimestamp();
   		logTrackerMsg( disconnectMsg );
    }
    
	@Subscribe
    public void onServerPostConnect( ServerPostConnectEvent event ) {

    	String switchMsg = "ServerSwitchEvent#" + event.getPlayer().getUsername() + "#ob-" + event.getPlayer().getCurrentServer().get().getServerInfo().getName() + "#" + getTimestamp();
    	logTrackerMsg( switchMsg );
    }  
    
    private void logTrackerMsg( String msg ) {
    	
    	try ( RandomAccessFile stream = new RandomAccessFile( OBMetaProducer.metafile, "rw" );
    		     FileChannel channel = stream.getChannel()) {

    		    // Use tryLock() or lock() to block until the lock is acquired
    		    FileLock lock = channel.lock();
    		    try {
    		        // Move to the end of the file to append data
    		        channel.position(channel.size());
    		        // Write data
    		        channel.write( ByteBuffer.wrap( ( msg + "\n" ).getBytes() ) );
    		    } finally {
    		        lock.release();
    		    }
    		} catch ( IOException e ) {
    		    logger.error("[" + program + "] Failed to write to metafile");
    		}		
	}
    
    private String getTimestamp() {
    	return new SimpleDateFormat( "MM/dd HH:mm:ss.ms" ).format( new Date() );

    }
}
