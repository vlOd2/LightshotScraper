package me.vlod.lightshotscraper;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

import javax.swing.JOptionPane;

import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import me.vlod.lightshotscraper.console.Console;
import me.vlod.lightshotscraper.logger.Logger;

public class LightshotScraper implements Runnable {
	public static LightshotScraper instance;
	public static Logger logger;
	public boolean running;
	public Console console;
	public String outputFolderBase;
	public boolean noSaving;
	public boolean noDownload;
	public String linkIDFormat = "cciiii";

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
	
	public void downloadImage(String url, String outputFilePath) {
		try {
			Response resultImageResponse = Jsoup.connect(url).ignoreContentType(true).execute();
			FileOutputStream imageStream = new FileOutputStream(new File(outputFilePath));
			imageStream.write(resultImageResponse.bodyAsBytes());
			imageStream.close();	
		} catch (Exception ex) {
			if (ex instanceof HttpStatusException && 
				((HttpStatusException)ex).getStatusCode() == 404) {
				logger.error(String.format("Link \"%s\" doesn't exist!", url));
			} else {
				logger.error(String.format("Unable to download the image from the link \"%s\"!", url));
				ex.printStackTrace();
			}
		}
	}
	
	public void handleInput(String input) {
		input = input.trim();
		if (input.length() < 1) {
			if (this.console != null) {
				JOptionPane.showMessageDialog(null, "Invalid input specified!", "LightshotScraper - Error", 
						JOptionPane.ERROR_MESSAGE | JOptionPane.OK_OPTION);
			} else {
				logger.error("Invalid input specified!");
			}
			
			return;
		}
		
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
				switch (cmd) {
        		case "generate":
        			if (arguments.length < 1) {
        				logger.error("Not enough arguments!");
        				break;
        			}
        			
        			String numberRaw = arguments[0];
        			int number = 0;
        			
        			try {
        				number = Integer.valueOf(numberRaw);
        			} catch (Exception ex) {
        				logger.error("Invalid arguments provided!");
        				break;
        			}
        			
        			long timeBeforeGen = System.currentTimeMillis();
        			String outputFolderPath = "";
        			
        			if (!noSaving) {
        				outputFolderPath = (outputFolderBase == null ? 
        						"." : outputFolderBase.trim()) + "/" + timeBeforeGen;
        				logger.info("Output folder: %s", outputFolderPath);
        				
        				// Create the output folder if it doesn't exist
        				try {
        					File outputFolder = new File(outputFolderPath);
        					if (!outputFolder.exists()) {
        						outputFolder.mkdir();
        					}	
        				} catch (Exception ex) {
        					logger.error("Unable to create the output folder! Do we have not write permissions?");
        					logger.error("Disabled file saving");
        					noSaving = true;
        					ex.printStackTrace();
        				}
        			}

        			String[] links = generateLinks(number);
        			PrintWriter fileDump = null;
        			if (!noSaving) {
        				try {
        					fileDump = new PrintWriter(new FileOutputStream(
        							new File(String.format("%s/links.html", outputFolderPath))));
        					fileDump.println("<!Doctype HTML>");
        					fileDump.println("<html>");
        					fileDump.println("\t<body>");
        					fileDump.println("\t\t<span>");
        				} catch (Exception ex) {
        					logger.error("Unable to create the HTML file! Do we have not write permissions?");
        					ex.printStackTrace();
        				}	
        			}

        			for (int i = 0; i < links.length; i++) {
        				String link = links[i];
        				String linkImageURL = "";
        				
        				// Print to console
        				logger.info("(%d) %s", i, link);
        				
        				if (!noSaving) {
        					// Get image URL
        					try {
        						Document document = Jsoup.connect(link).get();
        						Element element = document.getElementById("screenshot-image");
        						if (element == null) {
        							logger.error("Unable to get the image URL for the link %s!", link);
        						} else {
        							linkImageURL = element.attr("src");
        							// FIX: The schema is not present for some images
        							if (!linkImageURL.startsWith("http") && 
        								!linkImageURL.startsWith("https")) {
        								linkImageURL = String.format("https:%s", linkImageURL);
        							}
        						}
        					} catch (Exception ex) {
        						logger.error("Something went wrong whilst getting the"
        										+ " image URL for the link %s!", link);
        						ex.printStackTrace();
        					}
        					
        					// Write to file
        					if (fileDump != null) {
        						fileDump.println(String.format(
        								"\t\t\t(%d) <a href=\"%s\" target=_blank>%s</a>:<br>", i, link, link));
        						fileDump.println(String.format(
        								"\t\t\t<img style=\"color: red;\" src=\"%s\" alt=\"Link %d doesn\'t exist!\"/><br>", 
        								linkImageURL, i));
        					}
        					
        					// Download image URL
        					if (linkImageURL != "" && !noDownload) {
        						downloadImage(linkImageURL, String.format("%s/%d.png", 
        								outputFolderPath, i));
        					}	
        				}
        			}
        			
        			if (fileDump != null) {
        				fileDump.println("\t\t</span>");
        				fileDump.println("\t</body>");
        				fileDump.println("</html>");
        				fileDump.flush();
        				fileDump.close();
        			}
        			
        			long timeAfterGen = System.currentTimeMillis();
        			logger.info("Generated" + (noSaving || noDownload ? " " : " & downloaded ") + "%d links in %d seconds", 
        					links.length, (timeAfterGen - timeBeforeGen) / 1000);
        			
        			break;
        		case "togglenosaving":
        			noSaving = !noSaving;
        			logger.info("No saving is now %s", noSaving);
        			break;
        		case "togglenodownload":
        			noDownload = !noDownload;
        			logger.info("No download is now %s", noDownload);
        			break;
        		case "setoutputfolderbase":
        			if (arguments.length < 1) {
        				logger.error("Not enough arguments!");
        				break;
        			}
        			
        			outputFolderBase = arguments[0];
        			logger.info("Set output folder base to %s", outputFolderBase);
        			
        			break;
        		case "getlinkidformat":
        			logger.info(linkIDFormat);
        			break;
        		case "setlinkidformat":
        			if (arguments.length < 1) {
        				logger.error("Not enough arguments!");
        				break;
        			}
        			
        			linkIDFormat = arguments[0];
        			logger.info("Set link ID format to %s", linkIDFormat);
        			
        			break;
        		case "exit":
        			running = false;
        			if (console != null) {
        				System.exit(0);
        			}
        			break;
        		case "help":
        			logger.info("generate <number> - Generates x amount of links");
        			logger.info("setoutputfolderbase <path> - Sets the output folder base");
        			logger.info("togglenosaving - Toggles no saving");
        			logger.info("togglenodownload - Toggles no downloading");
        			logger.info("getlinkidformat - Prints the link ID format used for generation");
        			logger.info("setlinkidformat <format> - Sets the link ID format used for generation");
        			logger.info("clear - Clears the screen");
        			logger.info("exit - Exits LightshotScraper");
        			logger.info("");
        			logger.info("Link ID format information:");
        			logger.info("c - random a-z small character");
        			logger.info("C - random a-z big character");
        			logger.info("i - random 0-9 number");
        			logger.info("r - picks randomly between c, C and i");
        			break;
        		case "clear":
        			for (int i = 0; i < 1000; i++) {
        				System.out.println("");
        			}
        			if (console != null) {
        				console.clear();
        			}
        			printStartupMessage();
        			break;
        		default:
        			logger.error("Invalid command! Type \"help\" for more information");
        			break;
        		}
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
		
		Scanner inputScanner = null;
		if (System.console() != null) {
			inputScanner = new Scanner(System.in);
		}

		if (System.console() == null && !GraphicsEnvironment.isHeadless()) {
			this.console = new Console();
			this.console.onSubmit = new Delegate() {
				@Override
				public void call(Object... args) {
					logger.info("> %s", args[0]);
					handleInput((String)args[0]);
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
				String input = inputScanner.nextLine();
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
