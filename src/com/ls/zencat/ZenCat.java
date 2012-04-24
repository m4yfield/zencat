/*
    zencat - a zenoss irc bot
    Copyright (C) 2012 Katherine Daniels <katherine.daniels@livestream.com>

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; version 2 of the GPL only, not 3 :P

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package com.ls.zencat;

import org.apache.commons.configuration.*;
import org.jibble.pircbot.*;
import java.net.*;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.awt.Desktop;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONArray;

public class ZenCat extends PircBot {

	private String nick;
	private String cmdScript;
	private String defaultChannel = null;
	private int maxCmdResponseLines = 26;
	private XMLConfiguration config;

	private JsonApi ja = new JsonApi();

	public static void main(String[] args) throws Exception {
		try {
			if (args.length == 0) {
				System.out.println("first param should be config file");
				System.exit(-1);
			}
			XMLConfiguration c = null;
			try {
				c = new XMLConfiguration(args[0]);
			} catch (ConfigurationException cex) {
				System.err.println("Configuration error, check config file");
				cex.printStackTrace();
				System.exit(1);
			}

			ZenCat bot = new ZenCat(c);

			// listen for stuff and send it to irc:
			ServerSocket serverSocket = null;
			InetAddress inet = null;
			try {
				if (bot.getCatIP() != null)
					inet = InetAddress.getByName(bot.getCatIP());
			} catch (UnknownHostException ex) {
				System.out
						.println("Could not resolve config cat.ip, fix your config");
				ex.printStackTrace();
				System.exit(2);
			}

			try {
				serverSocket = new ServerSocket(bot.getCatPort(), 0, inet);
			} catch (IOException e) {
				System.err.println("Could not listen on port: "
						+ bot.getCatPort());
				System.exit(1);
			}

			System.out.println("Listening on " + bot.getCatIP() + " : "
					+ bot.getCatPort());

			while (true) {
				try {
					Socket clientSocket = serverSocket.accept();
					// System.out.println("Connection on catport from: "
					// + clientSocket.getInetAddress().toString());
					CatHandler handler = new CatHandler(clientSocket, bot);
					handler.start();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public ZenCat(XMLConfiguration c) throws Exception {
		this.config = c;
		setEncoding("UTF8");
		cmdScript = config.getString("script.cmdhandler");
		maxCmdResponseLines = config.getInt("script.maxresponselines", 26);
		nick = config.getString("bot.nick");
		setName(nick);
		setLogin(nick);
		setMessageDelay(config.getLong("bot.messagedelay", 1000));
		setFinger(config.getString("bot.finger",
				"ZenCat - a zenoss IRC bot"));


		try {
			// connect to server
			int tries =0 ;
			while (!isConnected()) {
				tries++;
				System.out.println("Connecting to server [try "+tries+"]: "+ config.getString("server.address"));
				connect(config.getString("server.address"), config.getInt(
						"server.port", 6667), config.getString(
						"server.password", ""));
				if(tries>1) Thread.sleep(10000);
			}
		} catch (Exception e) {
			System.out.println(e.toString());
		}

	}

	public String getCmdScript() {
		return cmdScript;
	}

	public int getCmdMaxResponseLines() {
		return maxCmdResponseLines;
	}

	protected void onDisconnect(){
      while (!isConnected()) {
         try {
            reconnect();
         }
         catch (Exception ex) {
            try {
               Thread.sleep(10000);
            } catch (InterruptedException e) {
               break;
            }
         }
      }
	}

	@SuppressWarnings("unchecked")
	protected void onConnect() {

		// join channels
		List<HierarchicalConfiguration> chans = config
				.configurationsAt("channels.channel");
		for (HierarchicalConfiguration chan : chans) {
			System.out.println("/join #" + chan.getString("name"));
			joinChannel("#" + chan.getString("name") + " "
					+ chan.getString("password", ""));
			// first one in the list considered default:
			if (defaultChannel == null)
				defaultChannel = "#"+chan.getString("name");
		}
        String nickpass = config.getString("server.identify","");
        if(nickpass != "")  identify(nickpass);
		
		System.out.println("Default channel: "+defaultChannel);
	}

	public int getCatPort() {
		return config.getInt("cat.port", 54321);
	}

	public String getCatIP() {
		return config.getString("cat.ip", "127.0.0.1");
	}

	public String getDefaultChannel() {
		return defaultChannel;
	}

	// PM was sent to us on irc
	public void onPrivateMessage(String sender, String login, String hostname,
			String message) {
		handleMessage(null, sender, message);
	}

    public void sendMsg(String t, String m) {
            m = mIRCify(m);
            super.sendNotice(t, m);
    }

    public String mIRCify(String m) {
            Map<String, String> colorReplacementMap = new HashMap<String, String>();
            colorReplacementMap.put("#NORMAL", Colors.NORMAL);
            colorReplacementMap.put("#BOLD", Colors.BOLD);
            colorReplacementMap.put("#UNDERLINE", Colors.UNDERLINE);
            colorReplacementMap.put("#REVERSE", Colors.REVERSE);
            colorReplacementMap.put("#WHITE", Colors.WHITE);
            colorReplacementMap.put("#BLACK", Colors.BLACK);
            colorReplacementMap.put("#DBLUE", Colors.DARK_BLUE);
            colorReplacementMap.put("#DGREEN", Colors.DARK_GREEN);
            colorReplacementMap.put("#RED", Colors.RED);
            colorReplacementMap.put("#BROWN", Colors.BROWN);
            colorReplacementMap.put("#PURPLE", Colors.PURPLE);
            colorReplacementMap.put("#ORANGE", Colors.OLIVE);
            colorReplacementMap.put("#YELLOW", Colors.YELLOW);
            colorReplacementMap.put("#GREEN", Colors.GREEN);
            colorReplacementMap.put("#TEAL", Colors.TEAL);
            colorReplacementMap.put("#CYAN", Colors.CYAN);
            colorReplacementMap.put("#BLUE", Colors.BLUE);
            colorReplacementMap.put("#PINK", Colors.MAGENTA);
            colorReplacementMap.put("#DGRAY", Colors.DARK_GRAY);
            colorReplacementMap.put("#GRAY", Colors.LIGHT_GRAY);

            for(Map.Entry<String, String> e : colorReplacementMap.entrySet())
                    m = m.replaceAll(e.getKey(), e.getValue());
            return m;
    }

	// message sent to our channel
	public void onMessage(String channel_, String sender, String login,
			String hostname, String message) {
		handleMessage(channel_, sender, message);
	}

	public void onPart(String _channel, String _sender, String _login,
			String _hostname) {
		if (!_sender.equals(nick))
			return;
		// System.out.println("Exiting due to onPart()");
		// System.exit(-1);
	}

	public void onQuit(String _sourceNick, String _sourceLogin,
			String _sourceHostname, String _reason) {
		if (!_sourceNick.equals(nick))
			return;
		System.out.println("Exiting due to onQuit()");
		System.exit(-1);
	}

	public void onKick(String channel_, String kickerNick, String kickerLogin,
			String kickerHostname, String recipientNick, String reason) {
		if (!recipientNick.equals(nick))
			return;

		// we were kicked
	}

	// is this nick trusted? (are they in the default channel)
	private boolean isTrusted(String n){
		//return true
		User[] users = getUsers(getDefaultChannel());
		for(int j =0; j<users.length; j++)
			if(n.equalsIgnoreCase(users[j].getNick()))  return true;
		
		return false;
	}
	
	public void handleMessage(String channel_, String sender, String message) {
		String cmd;
		String respondTo = channel_ == null ? sender : channel_;
		
		
		
		if (message.startsWith(nick)) {
			// someone said something to us.
			// we don't care!
			return;
		}

		if (message.startsWith("!")) {
			if(!isTrusted(sender)) {
                System.out.println("UNTRUSTED (ignoring): ["+respondTo+"] <"+sender+"> "+message);
                return;
            }
			// zencat builtin command processing:
			String resp = handleBuiltInCommand(message.substring(1).trim(),
					sender);
			if (!(resp == null || resp.equals("")))
				sendMessage(respondTo, resp);
            
            System.out.println("Built-in: ["+respondTo+"] <"+sender+"> "+message);
			return;
		}

		if (message.startsWith("?")) {
			// external script command.
			cmd = message.substring(1).trim();
		} else {
			// just a normal message which we ignore
			return;
		}

		if (cmd.trim().length() < 1)
			return;
		
		// if a PM, you gotta be trusted.
		if(channel_ == null && !isTrusted(sender)) {
            System.out.println("UNTRUSTED (ignoring): ["+respondTo+"] <"+sender+"> "+message);
            return;
        }
		
		// now "cmd" contains the message, minus the address prefix (eg: ?)
		// hand off msg to thread that executes shell script
        System.out.println("Scripter: ["+respondTo+"] <"+sender+"> "+message);
		Thread t = new Scripter(sender, channel_, respondTo, cmd, this);
		t.run();
	}

	/*
	 * Basic built-in command processing allows you to instruct the bot at
	 * runtime to join/leave channels etc
	 */
	protected String handleBuiltInCommand(String cmd, String sender) {
		String toks[] = cmd.split(" ");
		String method = toks[0];

		// JOIN A CHANNEL
		if (method.equals("join") && toks.length >= 2) {
			if (toks.length == 3)
				joinChannel(toks[1], toks[2]);
			else
				joinChannel(toks[1]);

			sendMessage(toks[1], "<" + sender + "> !" + cmd);
			return "Joining: " + toks[1];
		}

		// PART A CHANNEL
		if (method.equals("part") && toks.length == 2) {
			sendMessage(toks[1], "<" + sender + "> !" + cmd);
			partChannel(toks[1]);
			return "Leaving: " + toks[1];
		}

		// BROADCAST MSG TO ALL CHANNELS
		if (method.equals("spam")) {
			this.catStuffToAll("<" + sender + "> " + cmd.substring(5));
		}

		// LIST CHANNELS THE BOT IS IN
		if (method.equals("channels")) {
			String[] c = getChannels();
			StringBuffer sb = new StringBuffer("I am in " + c.length
					+ " channels: ");
			for (int i = 0; i < c.length; ++i)
				sb.append(c[i] + " ");
			return sb.toString();
		}

		// ACK A ZENOSS ALERT
		if (method.equals("ack") && toks.length == 2) {
			String evid = toks[1];
			try {
				JSONObject json = ja.ackEvent(evid);
				String resultString = new String();
				if (json.toString().indexOf("true") > 0) {
					resultString = "Successfully ACKed event: " + evid;
				} else {
					resultString = "Failed to ACK event: " + evid;
				}
				sendNotice("#noc", resultString);
			} catch (Exception e) {
				return e.getMessage();
			}
		}

		// UNACK A ZENOSS ALERT
		if (method.equals("unack") && toks.length == 2) {
                        String evid = toks[1];
                        try {
                                JSONObject json = ja.unAckEvent(evid);
                                String resultString = new String();
                                if (json.toString().indexOf("true") > 0) {
                                        resultString = "Successfully un-ACKed event: " + evid;
                                } else {
                                        resultString = "Failed to un-ACK event: " + evid;
                                }
                                sendNotice("#noc", resultString);
                        } catch (Exception e) {
                                return e.getMessage();
                        }
                }

		// SHOW ALL EVENTS
		if (method.equals("events")) {
			sendMessage("#noc", "ALL CURRENT ZENOSS EVENTS:");
			sendMessage("#noc", "severity | eventID | device: summary (state)");
			try {
				JSONObject json = ja.getEvents();
				// Loop through the array
				Long numEvents = (Long) json.get("totalCount");
				if (numEvents > 0) {
					JSONArray events = (JSONArray) json.get("events");
					for (int i=0; i<numEvents; i++) {
						JSONObject event = (JSONObject) events.get(i);
						String summary = event.get("summary").toString();
						String evid = event.get("id").toString();
						JSONObject device = (JSONObject) event.get("device");
						String deviceString = device.get("text").toString();
						String severity = event.get("severity").toString();
						String eventState = event.get("eventState").toString();
						String eventSummary = severity + " | " + evid + " | " + deviceString + ": " + summary + " (" + eventState + ")"; 
						sendMessage("#noc", eventSummary);
						//sendMessage("#noc", events.get(i).toString());
					}
				}
				//sendMessage("#noc", ja.getEvents().toString());
			} catch (Exception e) {
				return e.getMessage();
			}
		}

		if (method.equals("help")) {
			sendMessage("#noc", "MEOW MEOW MEOW MEOW");
			String helpText = new String();
			helpText = "I am the zencat. I send messages to IRC from Zenoss. You can also interact with Zenoss through me. To see all events, type !events. To acknowledge an event, type !ack [eventID].";
			sendMessage("#noc", helpText);
		}

		// EXIT()
		if (method.equals("exit"))
			System.exit(0);

		return "";
	}

	public void catStuffToAll(String stuff) {
		String[] channels = getChannels();
		for (int i = 0; i < channels.length; i++) {
			sendMsg(channels[i], stuff);
		}
	}

	public void catStuff(String stuff, String[] recips) {
		for (int ci = 0; ci < recips.length; ci++) {
			sendMsg(recips[ci], stuff);
		}
	}

}
