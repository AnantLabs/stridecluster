package com.lin.stride.hdfs;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
public final class HDFSFileSystem {

	private final Configuration conf = new Configuration();
	private FileSystem hdfs;
	private final Logger LOG = Logger.getLogger(HDFSFileSystem.class);
	private static final int BUFFERED_SIZE = 1024 * 4;
	private final String hdfsRoot = ConfigReader.getEntry("hadoop_index_path");
	private final String localIndexRoot = ConfigReader.getEntry("indexStorageDir");
	/* add 2012-10-16 xiaolin 更新时可能出现排队,上次所有集群还有机器没有更新下载完,这时又有新的....
	 * 不对,跑在2个jvm中,这个锁没有,写的锁是有作用的,读的锁没必要加上.只能通过zk中的状态来判断是否所有机器
	 * 都更新完数据,如果这时有上传索引的 ,就会导致更新索引的server异常,读取的文件不存在了,被删除了.
	 * */
	private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	public HDFSFileSystem() {
		try {
			hdfs = FileSystem.get(URI.create(ConfigReader.getEntry("hadoop_namenode_address")), conf);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	/**
	 * 上传本地的index文件.首先删除HDFS上的文件,删除后,添加本地的index文件到HDFS中.
	 * Date : 2012-9-25
	 * @throws IOException
	 */
	public void upLoadIndex() {
		FileStatus[] FileStatus;
		lock.writeLock().lock();
		try {
			FileStatus = hdfs.listStatus(new Path(hdfsRoot));
			for (FileStatus fs : FileStatus) {
				hdfs.delete(fs.getPath(), true);
			}
			File indexPath = new File(ConfigReader.getEntry("indexStorageDir"));
			File[] indexFiles = indexPath.listFiles();
			FileInputStream fis;
			FSDataOutputStream fsdos;
			for (File indexFile : indexFiles) {
				fis = new FileInputStream(indexFile);
				Path hdfsFile = new Path(hdfsRoot, indexFile.getName());
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
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * 下载HDFS上的文件,到本地的index目录下.
	 * Date : 2012-9-25
	 * @throws IOException
	 */
	public void downLoadIndex() {
		lock.readLock().lock();
		try {
			FileStatus[] FileStatus = hdfs.listStatus(new Path(hdfsRoot));
			FSDataInputStream fsis;
			FileOutputStream fos;
			for (FileStatus fs : FileStatus) {
				File localFile = new File(localIndexRoot, fs.getPath().getName());
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
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		} finally {
			lock.readLock().unlock();
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
		HDFSFileSystem hdfs = new HDFSFileSystem();
		hdfs.downLoadIndex();
		hdfs.close();
	}

}
