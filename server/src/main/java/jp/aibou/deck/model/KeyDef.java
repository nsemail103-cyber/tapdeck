package jp.aibou.deck.model;

/** A single key: position, label, icon and the action it triggers. */
public record KeyDef(String id, int row, int col, String title, Icon icon, Action action) {

    /** Icon shown on the key: an emoji, arbitrary text, or a stored image file name. */
    public record Icon(String type, String value) {
        public static Icon emoji(String value) {
            return new Icon("emoji", value);
        }

        public static Icon text(String value) {
            return new Icon("text", value);
        }

        /** value is a path relative to the server root, e. "icons/abc.png". */
        public static Icon image(String value) {
            return new Icon("image", value);
        }
    }
}
