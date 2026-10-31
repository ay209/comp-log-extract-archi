package hai913i.tp2.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CsvFactsSourceTest {

    @Test
    void microJeuCompteurs() throws IOException {
        FactsSource f = new CsvFactsSource(Path.of("src", "test", "resources"), "facts-mini");
        assertEquals(3, f.classNames().size());
        assertEquals(5, f.methodCount());
        assertEquals(4, f.arcs().size());
    }
}