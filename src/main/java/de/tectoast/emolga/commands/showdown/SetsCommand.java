package de.tectoast.emolga.commands.showdown;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.showdown.Analysis;
import de.tectoast.emolga.utils.showdown.Player;
import de.tectoast.emolga.utils.showdown.Pokemon;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SetsCommand extends Command {

    private static final Pattern BEGIN_RETURN = Pattern.compile("^\\r\\n");
    private static final Pattern END_RETURN = Pattern.compile("\\r\\n$");
    private final OkHttpClient client = new OkHttpClient().newBuilder().build();

    public SetsCommand() {
        super("sets", "Zeigt die Sets von einem Showdown-Kampf an", CommandCategory.Showdown);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("url", "Replay-Link", "Der Replay-Link", ArgumentManagerTemplate.Text.any())
                .setExample("!sets https://replay.pokemonshowdown.com/oumonotype-82345404").build());
        beta();
    }

    private static String buildPaste(Player p) {
        return p.getMons().stream().map(SetsCommand::buildPokemon).map(s -> END_RETURN.matcher(BEGIN_RETURN.matcher(s).replaceAll("")).replaceAll("")).collect(Collectors.joining("\r\n\r\n"));
    }

    private static String buildPokemon(Pokemon p) {
        return (p.getNickname().equals(p.getPokemon()) ? p.getPokemon() : p.getNickname() + " (%s)".formatted(p.getPokemon())) + p.buildGenderStr() + (p.getItem().map(s -> " @ " + s).orElse("")) + "  \r\n"
               + "Ability: " + p.getAbility().orElse("unknown") + "  \r\n"
               + p.getMoves().stream().map(s -> "- " + s + "  ").collect(Collectors.joining("\r\n"));
    }

    @Override
    public void process(GuildCommandEvent e) throws Exception {
        String url = e.getArguments().getText("url");
        e.reply(Arrays.stream(new Analysis(url, e.getMessage()).analyse()).map(p -> {
            try {
                String paste = buildPaste(p);
                //e.reply("```" + paste + "```");
                Response res = client.newCall(new Request.Builder()
                        .url("https://pokepast.es/create")
                        .method("POST", new FormBody.Builder()
                                .add("paste", paste)
                                .add("title", "Sets von " + p.getNickname())
                                .add("author", "Emolga")
                                .add("notes", url)
                                .build()
                        )
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .build()
                ).execute();
                String returl = res.request().url().toString();
                res.close();
                return p.getNickname() + ": " + returl;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return "";
        }).collect(Collectors.joining("\n")));
    }
}
