package com.lin.stride.hdfs;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;

import com.lin.stride.utils.ConfigReader;

/**
 * hdfs操作的工具类,但是hdfs的上传和下载是2个角色完成的工作,不是在一个节点上.
 * 所以没有写成静态方法,每次连接hdfs的时候,创建一个新的对象,使用完后立即关闭.
 * @Author : xiaolin
 * @Date : 2012-9-25 上午10:40:13
 */
public final class HDFSShcheduler {

	private final Configuration conf = new Configuration();
	private FileSystem hdfs;
	private final Logger LOG = Logger.getLogger(HDFSShcheduler.class);
	private static final int BUFFERED_SIZE = 1024 * 4;
	private final String hdfsRoot = ConfigReader.INSTANCE().getHadoopIndexPath();
	private final String localIndexRoot = ConfigReader.INSTANCE().getIndexStorageDir();

	public HDFSShcheduler() {
		try {
			String n = ConfigReader.INSTANCE().getHadoopNameNodeAddress();
			hdfs = FileSystem.get(URI.create(n), conf);
			if (!hdfs.exists(new Path(hdfsRoot))) {
				hdfs.mkdirs(new Path(hdfsRoot));
			}
			LOG.debug("HDFS init successful !");
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	public boolean exists(long version) throws IOException{
		Path latestDirectory = new Path(hdfsRoot + "/" + String.valueOf(version));
		return hdfs.exists(latestDirectory);
	}
	
	/**
	 * 上传一个根目录下的一个已日期为名称的子索引目录.上传前判断hdfs上是否有这个目录,如果有
	 * 清除这个目录下的文件,如果没有直接上传,上传后应该清除旧的数据,这个策略还没有添加.
	 * Date : 2012-9-25 modifyed:2012-12-26
	 * @throws IOException
	 */
	public void upLoadIndex(long version) throws IOException {
		Path latestDirectory = new Path(hdfsRoot + "/" + String.valueOf(version));
		if (!hdfs.exists(latestDirectory)) {
			hdfs.mkdirs(latestDirectory);
		} else {
			FileStatus[] fileStatus;
			fileStatus = hdfs.listStatus(latestDirectory);
			for (FileStatus fs : fileStatus) {
				hdfs.delete(fs.getPath(), true);
				LOG.debug("deleted file : " + fs.toString());
			}
		}
		File indexPath = new File(ConfigReader.INSTANCE().getIndexStorageDir(), String.valueOf(version));
		File[] indexFiles = indexPath.listFiles();
		FileInputStream fis;
		FSDataOutputStream fsdos;
		for (File indexFile : indexFiles) {
			fis = new FileInputStream(indexFile);
			Path hdfsFile = new Path(hdfsRoot + "/" + String.valueOf(version), indexFile.getName());
			fsdos = hdfs.create(hdfsFile);
			BufferedInputStream bis = new BufferedInputStream(fis);
			BufferedOutputStream bos = new BufferedOutputStream(fsdos);
			byte[] b = new byte[BUFFERED_SIZE];
			int n = 0;
			while ((n = bis.read(b)) != -1) {
				bos.write(b, 0, n);
			}
			LOG.info("upLoad ... " + FileUtils.byteCountToDisplaySize(indexFile.length()) + "\t" + hdfsFile.toString());
			bos.close();
			bis.close();
			fsdos.close();
			fis.close();
		}
	}

	public FileStatus[] listFile() throws IOException {
		FileStatus[] fileStatus = hdfs.listStatus(new Path(hdfsRoot));
		return fileStatus;
	}

	/**
	 * 获得HDFS上最新的索引版本.
	 * Date : 2012-12-26 下午1:18:02
	 * @return
	 * @throws IOException
	 */
	@Deprecated
	public long latestVersion() throws IOException {
		FileStatus[] fileStatus = listFile();
		long maxVersion = 0;
		long cursor = 0;
		for (FileStatus f : fileStatus) {
			cursor = Long.parseLong(f.getPath().getName());
			if (cursor > maxVersion) {
				maxVersion = cursor;
			}
		}
		return maxVersion;
	}

	/**
	 * 下载HDFS上的文件,到本地的index目录下.
	 * Date : 2012-9-25
	 * @throws IOException
	 */
	public void downLoadIndex(long childPathName) throws IOException {
		File indexDir = new File(localIndexRoot, String.valueOf(childPathName));
		if (!indexDir.exists()) {
			indexDir.mkdir();
		} else {
			FileUtils.cleanDirectory(indexDir);
		}
		FileStatus[] fileStatus = hdfs.listStatus(new Path(hdfsRoot, String.valueOf(childPathName)));
		FSDataInputStream fsis;
		FileOutputStream fos;
		for (FileStatus fs : fileStatus) {
			File localFile = new File(indexDir, fs.getPath().getName());
			fos = new FileOutputStream(localFile);
			fsis = hdfs.open(fs.getPath());
			BufferedInputStream bis = new BufferedInputStream(fsis);
			BufferedOutputStream bos = new BufferedOutputStream(fos);

			byte[] b = new byte[BUFFERED_SIZE];

			int n = 0;
			while ((n = bis.read(b)) != -1) {
				bos.write(b, 0, n);
			}
			LOG.info("download ... " + FileUtils.byteCountToDisplaySize(fs.getLen()) + "\t" + localFile.getAbsolutePath());
			bos.close();
			bis.close();
			fos.close();
			fsis.close();
		}
	}

	/**
	 * 关闭hdfs数据流
	 * Date : 2012-9-25
	 */
	public void close() {
		try {
			if (hdfs != null) {
				hdfs.close();
			}
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	public static void main(String[] args) throws Exception {
		HDFSShcheduler hdfs = new HDFSShcheduler();
		hdfs.close();
	}

}
