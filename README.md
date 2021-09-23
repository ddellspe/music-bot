# Music Bot
Music Bot is a site/discord bot set up to provide soundtrack-like support for discord servers.
Information about the tools and even how to run your own bot can be found below.
This Music bot provides you self-hosted capabilities similar to what [Groovy](https://groovy.bot/)
and [Rythm](https://rythm.fm/) did (before YouTube shut them down).
This project will slowly build items over time and accept pull request to update the code or add new features.

## MusicBot
To Run the music bot, you can run the docker image deployed to this repository with the following command:
```shell
docker run -d \ 
  --name music-bot \
  -e BOT_TOKEN=<YOUR BOT TOKEN> \
  -v </path/to/configs>:/app/resources/configs/ \
  -p 8080:8080
  ghcr.io/ddellspe/music-bot 
```

### Configuration

Without the configs folder mapped, the bot will only work in a test server personal to @ddellspe.
The current configuration of the config file is:
```json
[
  {
    "guildId": 123456789,
    "chatChannelId": 234567891,
    "prefix": "<DESIRED_PREFIX>"
  }
]
```
You can get the ids in Discord by enabling "developer" mode in settings => Advanced => Developer Mode.
Once developer mode is enabled, right-clicking on channels and discord servers (guilds) will provide a "Copy ID" option.
IDs should be the numeric value, the prefix must be a string.
Your prefix may be any string, but suggestions would be `"!"`, `">"`, or something like `"bot "`.
This prefix will have to prefix all commands (below) via chat message.

## Commands
Below is the listing for all commands, examples will be presented using the `"!"` prefix as an example.
All text-based commands must be sent from the channel aligning with the `chatChannelId` in the configuration.
If the message comes from any other message channel (chat channel) it will be ignored.

### Ping
| Command |
| :-: |
| `!ping` |

This is a command to test that the bot is up or alive, and that it's present in your Discord server.
When you request `!ping` the bot will respond in the channel with `Pong`.

### Start
| Command |
| :-: |
| `!start` |

This command starts the audio portion of the bot. 
To initiate the command, first join a voice channel, and then in the specified chat channel, send the message `!start`.
If the bot is able to join the voice channel that you are in, it will join the channel and report back the channel that it is in.
If the bot is unable to join the voice channel that you are in, it will wait roughly 10 seconds and report back that it was unable to join.
The bot, when added to your server, may need to be tagged with special roles to join voice channels.
Some private voice channels may be limited to a role scope, if so, you will need to add that role to the bot account.
If you wish to restrict the bot from certain channels, you can do so by role assignment as well.

### End
| Command |
| :-: |
| `!end` |

This commands stops the audio portion of the bot.
To initiate this command, in the specified chat channel, send the message `!end`.
Upon calling of this command, the bot (if running) will stop all audio, clear the queue, and then shut down.
When shutting down, the bot will leave the voice channel that it is connected to.
If the music bot isn't already running, nothing will happen.

### Silence
| Command |
| :-: |
| `!silence` |

This command stops the audio playback for the bot.
To initiate this command, in the specific chat channel send the message `!silence`.
Upon calling of this command, the bot (if running) will stop all audio, and clear the queue.
This does not end the bot's presence in the voice channel.
Any call to add new songs will immediately start their playing.

### Play
| Command |
| :-: |
| `!play <audio url>` |

This command adds a song (currently only one song) to the internal playlist and begins playback.
If there is no audio currently playing, playback will begin as soon as song resolution takes place.
In there is audio currently playing, the song will be added to the internal playlist.
There is currently no support for generic searching via keyboard at this point, so you must provide a full url.
When the bot finds the appropriate track to go along with the url, it will respond that it's been added to the queue.
There will also be track information (Title, Artist, Duration) available in the response from the command.
If the track isn't accessible or not found, the bot will provide an error response indicating such.

### Skip
| Command |
| :-: |
| `!skip` |

This command ends the current song, if there is another song in the queue and moves to the next song.
To initiate this command, in the specific chat channel send the message `!skip`.
Upon calling of this command, the bot (if running) will skip to the next track.
If there is no next track in the queue, it will send a message indicating that there is no track to skip to.
If there is a next track, the bot will skip to the next track and the information for that track will be shown.