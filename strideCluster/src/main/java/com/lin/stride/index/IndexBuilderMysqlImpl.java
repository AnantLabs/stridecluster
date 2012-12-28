package com.lin.stride.index;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;

import com.lin.stride.utils.ConfigReader;
import com.lin.stride.utils.ZKIndexVersionTools;

public class IndexBuilderMysqlImpl implements IndexBuilder {
	private final Logger LOG = Logger.getLogger(IndexBuilderMysqlImpl.class);
	private Connection dbConnection = null;
	private int minid;
	private int maxid;
	private final Statement stat;
	private final Directory dir;
	private final IndexWriterConfig config;
	private final IndexWriter indexWriter;
	private final long currentIndexDir;

	public IndexBuilderMysqlImpl() throws Exception {

		Class.forName("com.mysql.jdbc.Driver");
		dbConnection = DriverManager.getConnection("jdbc:mysql://120.197.94.139/noveladmin", "wap.news", "qazwsx");
		stat = dbConnection.createStatement();
		ResultSet res = stat.executeQuery(("SELECT MIN(novelid) from novel"));
		if (res.next()) {
			minid = res.getInt(1) - 1;
		}
		res = stat.executeQuery(("SELECT MAX(novelid) from novel"));
		if (res.next()) {
			maxid = res.getInt(1);
		}
		maxid = 200000;
		LOG.info("minid: " + minid + "\t maxid: " + maxid);
		res.close();

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		currentIndexDir = Long.parseLong(sdf.format(new Date()));
		File storageDir = new File(ConfigReader.INSTANCE().getIndexStorageDir() + File.separator + currentIndexDir);

		dir = MMapDirectory.open(storageDir);
		config = new IndexWriterConfig(Version.LUCENE_40, new StandardAnalyzer(Version.LUCENE_40));
		config.setOpenMode(OpenMode.CREATE);
		config.setRAMBufferSizeMB(100);
		indexWriter = new IndexWriter(dir, config);
	}

	@Override
	public byte[] build() throws Exception {
		Document doc = new Document();
		TextField name = new TextField("name", "", Store.YES);
		TextField author = new TextField("author", "", Store.YES);
		TextField tag = new TextField("tag", "", Store.YES);
		IntField intime = new IntField("intime", 0, Store.YES);
		IntField chapter_count = new IntField("chaptercount", 0, Store.YES);

		doc.add(name);
		doc.add(author);
		doc.add(tag);
		doc.add(intime);
		doc.add(chapter_count);

		String sql = "select n.name name ,a.name author, n.tag, n.intime, n.chapter_count from novel n left join author a on n.authorid=a.authorid where n.novelid between ? and ?";
		PreparedStatement ps = dbConnection.prepareStatement(sql);
		int num = 0;
		String nameValue;
		Date intimeValue;

		while (minid < maxid) {
			ps.setInt(1, minid + 1);
			minid = minid + 10000;
			ps.setInt(2, minid);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				nameValue = rs.getString("name");
				if (nameValue != null) {

					num++;
					if (num % 10000 == 0) {
						LOG.info("Cursor to :" + num);
					}

					name.setStringValue(nameValue);
					author.setStringValue(rs.getString("author") == null ? "" : rs.getString("author"));
					tag.setStringValue(rs.getString("tag") == null ? "" : rs.getString("tag"));
					intimeValue = rs.getDate("intime");
					intime.setIntValue(((Long) (intimeValue.getTime() / (1000 * 60 * 60))).intValue());
					chapter_count.setIntValue(rs.getInt("chapter_count"));
					indexWriter.addDocument(doc);
				}
			}
			rs.close();
		}
		int maxDoc = indexWriter.maxDoc();
		LOG.info("文档总数:" + maxDoc);
		indexWriter.close();
		dir.close();
		return ZKIndexVersionTools.versionToBytes(maxDoc, currentIndexDir);
	}

	@Override
	public byte[] rebuild() throws Exception {
		return build();
	}

	@Override
	public void close() {
		try {
			dbConnection.close();
		} catch (SQLException e) {
			LOG.error(e.getMessage(), e);
		}

	}

	public static void main(String[] args) throws Exception {
		IndexBuilderMysqlImpl ip = new IndexBuilderMysqlImpl();
		ip.build();
		ip.close();
	}

}
