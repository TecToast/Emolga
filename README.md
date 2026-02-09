# Emolga

Emolga is a discord bot based on the [JDA](https://github.com/discord-jda/JDA) library, which is mainly focused on Pokémon.
The main feature is the analysis of Pokémon Showdown! replays.
It has also lots of features helping to manage a draft league, like hosting a draft with a timer, automatic insertion of
picked mons into a google sheet and a lot more, which are currently used in a few draft leagues and will be made public
available someday in the future.

The bot is available in German and English.

The bot also has a [website](https://emolga.tectoast.de) (source code [here](https://github.com/TecToast/emolgaweb)).

Contributions are welcome, feel free to open an issue or a pull request.
For contact, you can join [Emolga's discord server](https://discord.gg/WYfKHPCgs9) or contact me directly on discord
(`@tectoast`).

## Development

If you really want to set up a development, feel free to contact me, I will be happy to help you.
You need to run the `generateK18nCode` gradle task to generate the code for the internationalization, which is used in
the bot.

### New languages

If you want to add a new language, you will have to create a `k18n/<language>.json` file, which contains the
translations for the bot. You can use the existing `k18n/de.json` file as a template.