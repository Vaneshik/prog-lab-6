package network;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

@AllArgsConstructor
@ToString
@Getter
public class Request implements Serializable {
    private final String commandName;
    private final String[] commandStrArg;
    private final Serializable commandObjArg;

    public Request(String commandName, String[] commandStrArg) {
        this(commandName, commandStrArg, null);
    }

    public Request() {
        this("", new String[0], null);
    }
}
