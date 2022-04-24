package net.ddellspe.music.bot.commands;

/**
 * This is a command in response to a chat message that requires a specific role to be present to
 * operate correctly, currently this is handled at the listener level, but it could be configurable
 * in the future.
 */
public interface SuperUserMessageResponseCommand extends MessageResponseCommand {}
