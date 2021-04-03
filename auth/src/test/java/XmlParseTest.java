import org.comroid.uniform.adapter.xml.jackson.JacksonXMLAdapter;
import org.comroid.uniform.node.UniNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public final class XmlParseTest {
    public static void main(String[] args) throws IOException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();

        InputStream resource = classLoader.getResourceAsStream("org.comroid.webkit/frame.html");
        System.out.println("resource = " + resource);

        String content;
        try (
                InputStreamReader isr = new InputStreamReader(resource);
                BufferedReader br = new BufferedReader(isr)
        ) {
            content = br.lines().collect(Collectors.joining("\n"));
        }

        UniNode node = JacksonXMLAdapter.instance.parse(content);
        System.out.println("node = " + node);
    }
}
