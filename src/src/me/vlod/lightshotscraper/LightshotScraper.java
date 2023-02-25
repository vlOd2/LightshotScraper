package me.vlod.lightshotscraper;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

import javax.swing.JOptionPane;

import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;

import me.vlod.discord.webhook.DiscordWebhook;
import me.vlod.lightshotscraper.console.Console;
import me.vlod.lightshotscraper.logger.Logger;

public class LightshotScraper implements Runnable {
	public static LightshotScraper instance;
	public static Logger logger;
	public boolean running;
	public Console console;
	public CommandHandler commandHandler;
	public String outputFolderBase;
	public boolean noSaving;
	public boolean noDownload;
	public String linkIDFormat = "cciiii";
	public DiscordWebhook discordWebhook;

	static {
		// Logger setup
		logger = new Logger();
		
		// Console target
		logger.targets.add(new Delegate() {
			@Override
			public void call(Object... args) {
				String str = (String) args[0];
				Color color = (Color) args[1];
				
				if (instance != null && instance.console != null) {
					instance.console.write(str, color);
				}
			}
		});
		
		// Stdout and Stderr target
		logger.targets.add(new Delegate() {
			@Override
			public void call(Object... args) {
				String str = (String) args[0];
				Color color = (Color) args[1];
				
				if (color != Color.red) {
					System.out.println(str);
				} else {
					System.err.println(str);
				}
			}
		});
	}
	
	public String[] generateLinks(int amount) {
		String chars = "abcdefghijklmnoprqstuwvxyz";
		String bigChars = chars.toUpperCase();
		ArrayList<String> links = new ArrayList<String>();
		ThreadLocalRandom random = ThreadLocalRandom.current();
		
		for (int i = 0; i < amount; i++) {
			String link = "https://prnt.sc/";
			
			for (char chr : this.linkIDFormat.toCharArray()) {
				if (chr == 'c') {
					link += chars.charAt(random.nextInt(0, chars.length()));
				} else if (chr == 'C') {
					link += bigChars.charAt(random.nextInt(0, bigChars.length()));
				} else if (chr == 'i') {
					link += "" + random.nextInt(0, 10);
				} else if (chr == 'r') {
					int chance = random.nextInt(0, 3);

					if (chance == 0) {
						link += bigChars.charAt(random.nextInt(0, bigChars.length()));
					} else if (chance == 1) {
						link += chars.charAt(random.nextInt(0, chars.length()));
					} else {
						link += "" + random.nextInt(0, 10);
					}
				}
			}
			
			links.add(link);
		}
		
		return links.toArray(new String[0]);
	}
	
	public boolean downloadImage(String url, String outputFilePath) {
		try {
			Response resultImageResponse = Jsoup.connect(url).ignoreContentType(true).execute();
			FileOutputStream imageStream = new FileOutputStream(new File(outputFilePath));
			imageStream.write(resultImageResponse.bodyAsBytes());
			imageStream.close();
			return true;
		} catch (Exception ex) {
			if (ex instanceof HttpStatusException && 
				((HttpStatusException)ex).getStatusCode() == 404) {
				logger.error(String.format("Link \"%s\" doesn't exist!", url));
			} else {
				logger.error(String.format("Unable to download the image from the link \"%s\"!", url));
				ex.printStackTrace();
			}
			return false;
		}
	}
	
	public void handleInput(String input) {
		if (input.length() < 1) return;
		
		String[] inputSplitted = Utils.splitBySpace(input);
		String cmd = inputSplitted[0];
		String[] arguments = new String[inputSplitted.length - 1];
		System.arraycopy(inputSplitted, 1, arguments, 0, arguments.length);
		
        for (int argIndex = 0; argIndex < arguments.length; argIndex++) {
            String arg = arguments[argIndex];

            if (arg.startsWith("\"")) {
            	arg = arg.substring(1, arg.length() - 1);
            }
                
            if (arg.endsWith("\"")) {
            	arg = arg.substring(0, arg.length() - 1);
            }
            
            arguments[argIndex] = arg;
        }

        Delegate handleInputDelegate = new Delegate() {
			@Override
			public void call(Object... args) {
				commandHandler.doCommand(cmd, arguments);
			}
        };
        
        if (this.console != null) {
            new Thread() {
            	@Override
            	public void run() {
            		handleInputDelegate.call();
            	}
            }.start();
        } else {
        	handleInputDelegate.call();
        }
	}
	
	public void printStartupMessage() {
		logger.info("Welcome to LightshotScraper!");
		logger.info("Type \"help\" for more information");
		logger.warn("WARNING: The photos you generate may contain pornography"
				+ " or other inappropriate imagery. Use with caution");
	}
	
	@Override
	public void run() {
		this.running = true;
		this.commandHandler = new CommandHandler(this);
	
		Scanner inputScanner = null;
		if (System.console() != null) {
			inputScanner = new Scanner(System.in);
		}

		if (System.console() == null && !GraphicsEnvironment.isHeadless()) {
			this.console = new Console();
			this.console.onSubmit = new Delegate() {
				@Override
				public void call(Object... args) {
					String input = (String)args[0];
					if (input.length() < 1) {
						JOptionPane.showMessageDialog(null, "Invalid input specified!", 
								"LightshotScraper - Error", 
								JOptionPane.ERROR_MESSAGE | JOptionPane.OK_OPTION);
						return;
					}
					logger.info("> %s", input);
					handleInput(input);
				}
			};
			this.console.onClose = new Delegate() {
				@Override
				public void call(Object... args) {
					System.exit(0);
				}
			};
			this.console.show();
		}
		
		this.printStartupMessage();
		while (this.running) {
			if (inputScanner != null) {
				System.out.print("> ");
				String input = inputScanner.nextLine().trim();
				if (input.length() < 1) {
					logger.error("Invalid input specified!");
					continue;
				}
				this.handleInput(input);
			}
		}
		
		if (inputScanner != null) {
			inputScanner.close();
		}
	}
	
	public static void main(String[] args) {
		new Thread(instance = new LightshotScraper()).start();
	}
}
