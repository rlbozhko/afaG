package com.afa.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class Utils {

	public static String getPageHtml(String url) {
		String html = "";
		try {
			URL siteUrl = new URL(url);
			HttpURLConnection httpURLConnection = (HttpURLConnection) siteUrl
					.openConnection();
			httpURLConnection.connect();

			InputStream inputStream = httpURLConnection.getInputStream();
			Scanner scanner = new Scanner(inputStream, "UTF-8");
			html = scanner.useDelimiter("\\A").next();
			scanner.close();
			httpURLConnection.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return html;
	}

}
