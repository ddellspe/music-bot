# Music Bot
[![Build](https://github.com/ddellspe/music-bot/actions/workflows/build.yml/badge.svg)](https://github.com/ddellspe/music-bot/actions/workflows/build.yml)
[![GitHub](https://img.shields.io/github/license/ddellspe/music-bot)](LICENSE)
![GitHub top language](https://img.shields.io/github/languages/top/ddellspe/music-bot)
[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/ddellspe/music-bot?sort=semver)](https://github.com/ddellspe/music-bot/releases/latest)

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

Without the configs folder mapped, the bot will not start up, you must take the 
[music_config.json.example](src/main/resources/configs/music_config.json.example), move it to `music_config.json` in 
the same folder and update the `guildId`, `chatChannelId` and `prefix` as necessary.
If you plan to host multiple discords in a single file, the config supports an array of configuration objects.
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

This command adds a song or full playlist to the internal bot playlist and begins playback.
If there is no audio currently playing, playback will begin as soon as song resolution takes place.
In there is audio currently playing, the song will be added to the internal playlist.
There is currently no support for generic searching via keyboard at this point, so you must provide a full url.
When the bot finds the appropriate track to go along with the url, it will respond that it's been added to the queue.
There will also be track information (Title, Artist, Duration) available in the response from the command.
If the track isn't accessible or not found, the bot will provide an error response indicating such.

### Force Play
| Command |
| :-: |
| `!fplay <audio url>` |

This command adds a song to be played immediately with the items in the playlist being queued immediately after.
If there is no audio currently playing, playback will begin as soon as song resolution takes place.
In there is audio currently playing, the song will begin as soon as song resolution takes place.
There is currently no support for generic searching via keyboard at this point, so you must provide a full url.
When the bot finds the appropriate track to go along with the url, it will respond that it's playing.
There will also be track information (Title, Artist, Duration) available in the response from the command.
If the track isn't accessible or not found, the bot will provide an error response indicating such.

### Interrupt
| Command |
| :-: |
| `!interrupt <audio url>` |

This command adds a song to be played immediately with the items in the playlist being queued immediately after with the currently playing song being re-queued.
If there is no audio currently playing, playback will begin as soon as song resolution takes place.
In there is audio currently playing, the song will begin as soon as song resolution takes place.
The currently playing song will be re-queued after the song/playlist at the existing position of the song at playback.
There is currently no support for generic searching via keyboard at this point, so you must provide a full url.
When the bot finds the appropriate track to go along with the url, it will respond that it's playing.
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
