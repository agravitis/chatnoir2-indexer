package org.clueweb.mapreduce;

import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * MapReduce Reducer for aggregating MapWritables.
 *
 * @author Janek Bevendorff
 * @version 1
 */
public class CluewebMapReducer extends Reducer<Text, MapWritable, NullWritable, MapWritable>
{
    /**
     * Template map writable containing all required entries.
     */
    private final MapWritable mTemplate;

    public CluewebMapReducer()
    {
        super();
        mTemplate = new MapWritable();
        mTemplate.put(new Text("WARC-TREC-ID") ,                new Text(""));
        mTemplate.put(new Text("WARC-Warcinfo-ID"),             new Text(""));
        mTemplate.put(new Text("WARC-Identified-Payload-Type"), new Text(""));
        mTemplate.put(new Text("WARC-Target-URI"),              new Text(""));
        mTemplate.put(new Text("meta_desc"),                    new Text(""));
        mTemplate.put(new Text("meta_keywords"),                new Text(""));
        mTemplate.put(new Text("anchor_texts"),                 new ArrayWritable(new String[0]));
        mTemplate.put(new Text("title"),                        new Text(""));
        mTemplate.put(new Text("body"),                         new Text(""));
        mTemplate.put(new Text("raw_html"),                     new Text(""));
        mTemplate.put(new Text("page_rank"),                    new FloatWritable(0.0f));
        mTemplate.put(new Text("spam_rank"),                    new LongWritable(0l));
    }

    @Override
    public void reduce(Text key, Iterable<MapWritable> values, Context context) throws IOException, InterruptedException {
        MapWritable outWritable = mTemplate;

        final Text anchorKey = new Text("anchor_texts");
        ArrayList<String> anchorTexts = new ArrayList<>();

        for (MapWritable value : values) {
            // accumulate anchor texts instead of overwriting
            if (value.keySet().contains(anchorKey)) {
                anchorTexts.add(value.get(anchorKey).toString());
                value.remove(anchorKey);
            }
            outWritable.putAll(value);
        }

        outWritable.put(anchorKey, new ArrayWritable(anchorTexts.toArray(new String[anchorTexts.size()])));

        // prettify Text fields by replacing broken Unicode replacement characters with zero-width spaces
        for (Writable k : outWritable.keySet()) {
            if (outWritable.get(k) instanceof Text) {
                outWritable.put(k, new Text(outWritable.get(k).toString().replaceAll("\ufffd", "\u200b")));
            }
        }

        context.write(NullWritable.get(), outWritable);
    }
}