# SpigotLanguageHelper

## About
This Library makes it easier for your Minecraft Plugins to support multiple Languages.
You can choose if you want to use local yaml files or store your translations in a database.

## Installation
Add using maven:
```
<repository>
  <id>jitpack.io</id>
  <url>https://jitpack.io</url>
</repository>
```
Current (recommended) Version:
```
<dependency>
  <groupId>com.github.Skiftstar</groupId>
  <artifactId>SpigotLanguageHelper</artifactId>
  <version>1.5.3-SNAPSHOT</version>
</dependency>
```
Way older version, but with static LanguageHelper:
```
<dependency>
  <groupId>com.github.Skiftstar</groupId>
  <artifactId>SpigotLanguageHelper</artifactId>
  <version>1.2-SNAPSHOT</version>
</dependency>
```

## Getting Started


Let's create a database object, this is only needed if you want to use a database
To store either the user data (what language they have selected) or the translations there
```
String host = config.getString("database.host");
int port = config.getInt("database.port");
String database = config.getString("database.database");
String user = config.getString("database.user");
String password = config.getString("database.password");
```
Last boolean indiciates whether or not we want to have only userdata (false) or also translations (true) stored in the DB
```
MariaDB helperDb = new MariaDB(host, port, user, password, database, true);
```
Make a Map with all the languages your plugin supports out of the Box (new ones can be added later by hand by creating new yml files or new db entries)
These files will be checked on Plugin start to add any missing keys
You will need atleast one of those in your plugin
```
Map<String, Reader> langs = new HashMap<>();
langs.put("de", getTextResource("de.yml"));
langs.put("en", getTextResource("en.yml"));
```

Now let's create the actual helper, here we set "de" as the defautl language, give it a prefix and our database.
If you don't plan on using a DB, you can omit the DB param
```
LanguageHelper helper = new LanguageHelper(this, "de", langs, Util.color(getConfig().getString("chatPrefix") + " "), helperDb);
```
This config option changes whether or not a message will be shown when a player joins that hasn't selected a language yet (and changes them to the default lang)
```
helper.setSendNoLangSetMess(false);
```

Now let's see how we can get a message with the helper
```
Player p = our_example_player;
String message = helper.getMess(player, "MessageKey");
```
By defautl the prefix we set will not be added, so if you want a prefix, use it like that
```
String messageWithPrefix = helper.getMess(player, "MessageKey", true);
```
When you don't have a player, you can use the following to get the message in the default language you set
```
helper.getMess("MessageKey", usePrefix);
```

Same works for lores
```
List<String> lore = helper.getLore("MessageKey");
List<String> lore = helper.getLore(player, "MessageKey");
```

## Language File Structure
The Plugin allows for your Language Files to have Segments, each message must have a Parent Key, a Message Key and a String value
The Parent Key is only for you to Structure the file
BUT if you want messages to be loaded as Lores, the Message Key must contain "lores"
An Example of a language file would be:
```
Error:
  PlayerOnly: "Player only!"
  InvalidGamemode: "&4%mode &cis not a valid gamemode!"
  NEPerms: "&cNot enough permissions!"
  NEArgs: "&cNot enough arguments!"
  PlayerNotFound: "&cPlayer &4%player &cwas not found!"
  InvalidCoordinates: "&4%cords &care not coordinates!"
  LuckPermsCantChangeThis: "&cYou can't edit &4%target &cbecause your rank isn't high enough!"

Sidebar:
  SidebarTitle: "%ServerName"

Sidebarlores:
  Sidebar:
    - "&7» Rank"
    - "%rankName"
    - " "
    - "&7» Online: %players"
    - "  "
    - "&7» Money: %money"

JoinLeave:
  Join: "&8[&a+&8] &7%player"
  Leave: "&8[&c-&8] &7%player"

Chat:
  ChatTemplate: "&7%prefix%player &8» &7%message"

Mute:
  PermaMuteMessage: "&cYou've been &4PERMANENTLY &cmuted!\n&cReason: &4%reason"
  MuteMessage: "&cYou've been muted for &4%duration!\n&cReason: &4%reason"

Constants:
  "Days": "Days"
  "Months": "Months"
  "Hours": "Hours"
  "Minutes": "Minutes"
  "Seconds": "Seconds"
```
