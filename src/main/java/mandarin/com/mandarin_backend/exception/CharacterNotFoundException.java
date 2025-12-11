package mandarin.com.mandarin_backend.exception;

public class CharacterNotFoundException extends RuntimeException {
    public CharacterNotFoundException(String msg) {
        super(msg);
    }
}
