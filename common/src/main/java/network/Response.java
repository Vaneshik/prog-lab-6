package network;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import java.io.Serializable;

@AllArgsConstructor
@Getter
@ToString
public class Response implements Serializable {
    private String message;
    private String collectionToStr;
}
