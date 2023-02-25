package me.vlod.lightshotscraper.discord.webhook;

public class DiscordEmbedImage {
	public String url;
	public int width;
	public int height;
	
	public DiscordEmbedImage(String url, int width, int height) {
		this.url = url;
		this.width = width;
		this.height = height;
	}
	
	public String getUrl() {
		return this.url;
	}
	
	public int getWidth() {
		return this.width;
	}
	
	public int getHeight() {
		return this.height;
	}
}
