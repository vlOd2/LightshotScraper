package me.vlod.discord.webhook;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.json.JSONObject;

public class DiscordWebhook {
	private String url;
	private String content;
	private String avatarURL;
	private ArrayList<DiscordEmbed> embeds = new ArrayList<DiscordEmbed>();
	
	public DiscordWebhook(String url) {
		this.url = url;
	}
	
	public void setContent(String content) {
		this.content = content;
	}
	
	public void setAvatarURL(String avatarURL) {
		this.avatarURL = avatarURL;
	}
	
	public void addEmbed(DiscordEmbed embed) {
		this.embeds.add(embed);
	}
	
	public void clearEmbeds() {
		this.embeds.clear();
	}
	
	public int submit() {
		if (this.content == null && this.embeds.size() < 1) {
			throw new IllegalStateException();
		}
		
		try {
			HttpURLConnection httpConnection = (HttpURLConnection) new URL(url).openConnection();
			httpConnection.setRequestMethod("POST");
			httpConnection.setRequestProperty("Content-Type", "application/json");
			httpConnection.setRequestProperty("User-Agent", "DiscordBot");
			httpConnection.setDoOutput(true);
			
			OutputStream outputStream = httpConnection.getOutputStream();
			PrintWriter printWriter = new PrintWriter(outputStream);
			
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("content", this.content);
			jsonObject.put("avatar_url", this.avatarURL);
			
			for (DiscordEmbed embed : this.embeds) {
				jsonObject.append("embeds", new JSONObject(embed));
			}
			
			printWriter.println(jsonObject.toString());
			printWriter.flush();
			printWriter.close();
			
			this.setContent(null);
			this.clearEmbeds();

			return httpConnection.getResponseCode();
		} catch (Exception ex) {
			return -1;
		}
	}
}
