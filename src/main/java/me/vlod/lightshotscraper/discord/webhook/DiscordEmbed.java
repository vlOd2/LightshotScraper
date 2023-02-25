package me.vlod.lightshotscraper.discord.webhook;

public class DiscordEmbed {
	public String title;
	public String description;
	public int color;
	public DiscordEmbedFooter footer;
	public DiscordEmbedImage image;
	public DiscordEmbedAuthor author;
	
	public DiscordEmbed(String title, String description, int color,
			DiscordEmbedFooter footer, DiscordEmbedImage image, DiscordEmbedAuthor author) {
		this.title = title;
		this.description = description;
		this.color = color;
		this.footer = footer;
		this.image = image;
		this.author = author;
	}
	
	public String getTitle() {
		return this.title;
	}
	
	public String getDescription() {
		return this.description;
	}
	
	public int getColor() {
		return this.color;
	}
	
	public DiscordEmbedFooter getFooter() {
		return this.footer;
	}
	
	public DiscordEmbedImage getImage() {
		return this.image;
	}
	
	public DiscordEmbedAuthor getAuthor() {
		return this.author;
	}
}