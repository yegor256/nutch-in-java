/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Yegor Bugayenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.yegor256.nutch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.nutch.crawl.CrawlDb;
import org.apache.nutch.crawl.Generator;
import org.apache.nutch.crawl.Injector;
import org.apache.nutch.fetcher.Fetcher;
import org.apache.nutch.parse.ParseSegment;
import org.apache.nutch.tools.FileDumper;
import org.apache.nutch.util.NutchConfiguration;

/**
 * Main entry point.
 *
 * @author Yegor Bugayenko (yegor256@gmail.com)
 * @version $Id: 098df17faf90bd370f83b49621ec6be614694547 $
 * @since 0.1
 * @checkstyle JavadocVariableCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class Main {

    /**
     * Ctor.
     */
    private Main() {
        // nothing
    }

    /**
     * Main Java entry point.
     * @param args Command line args
     * @throws Exception If fails
     */
    public static void main(final String... args) throws Exception {
        final Configuration conf = NutchConfiguration.create();
        conf.set("http.agent.name", "yc");
        conf.set("plugin.folders", System.getProperty("nutch.plugins.dir"));
        final Path home = new Path("target");
        FileUtils.forceMkdir(new File(home.toString()));
        final Path targets = new Path(home, "urls");
        Files.createDirectory(Paths.get(targets.toString()));
        final String[] urls = {"http://www.zerocracy.com"};
        Files.write(
            Paths.get(targets.toString(), "list-of-urls.txt"),
            String.join("\n", urls).getBytes()
        );
        new Injector(conf).inject(
            new Path(home, "crawldb"),
            new Path(home, "urls"),
            true, true
        );
        for (int idx = 0; idx < 2; ++idx) {
            Main.cycle(home, conf);
        }
        Files.createDirectory(Paths.get(new Path(home, "dump").toString()));
        new FileDumper().dump(
            new File(new Path(home, "dump").toString()),
            new File(new Path(home, "segments").toString()),
            null, true, false, true
        );
    }

    /**
     * One cycle.
     * @param home The dir
     * @param conf Config
     * @throws Exception If fails
     */
    private static void cycle(final Path home,
        final Configuration conf) throws Exception {
        final Path segments = new Path(home, "segments");
        new Generator(conf).generate(
            new Path(home, "crawldb"),
            new Path(home, "segments"),
            // @checkstyle MagicNumber (1 line)
            1, 1000L, System.currentTimeMillis()
        );
        final Path sgmt = Main.segment(segments);
        new Fetcher(conf).fetch(
            // @checkstyle MagicNumber (1 line)
            sgmt, 10
        );
        new ParseSegment(conf).parse(sgmt);
        new CrawlDb(conf).update(
            new Path(home, "crawldb"),
            Files.list(Paths.get(segments.toString()))
                .map(p -> new Path(p.toString()))
                .toArray(Path[]::new),
            true, true
        );
    }

    /**
     * Get segment to work with.
     * @param dir Dir with all of them
     * @return Get the latest
     * @throws IOException If fails
     */
    private static Path segment(final Path dir) throws IOException {
        final List<Path> list = Files.list(Paths.get(dir.toString()))
            .map(p -> new Path(p.toString()))
            .sorted(Comparator.comparing(Path::toString))
            .collect(Collectors.toList());
        return list.get(list.size() - 1);
    }
}
