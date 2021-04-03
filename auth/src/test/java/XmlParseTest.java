import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public final class XmlParseTest {
    public static void main(String[] args) throws IOException {
        Document frame = Jsoup.parse(getResourceContent("org.comroid.webkit/frame.html"));
        Document footer = Jsoup.parse(getResourceContent("org.comroid.webkit/part/footer.html"));

        Elements footerTag = frame.getElementsByTag("footer");
        footerTag.html('\n' + footer.html());

        System.out.println("frame = " + frame);
    }

    public static String getResourceContent(String resourceName) throws IOException {
        InputStream resource = ClassLoader.getSystemClassLoader().getResourceAsStream(resourceName);
        System.out.println("resource = " + resource);

        String content;
        try (
                InputStreamReader isr = new InputStreamReader(resource);
                BufferedReader br = new BufferedReader(isr)
        ) {
            content = br.lines().collect(Collectors.joining("\n"));
        }
        return content;
    }
}
