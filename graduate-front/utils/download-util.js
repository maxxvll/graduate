/**
 * 聊天文件下载工具类（PC端H5专属）
 * 功能：文件缓存索引、持久化管理、重复下载拦截、本地路径读取、用户信息持久化
 */
// 【关键修复1：默认关闭自动下载，彻底杜绝刷新自动下载文件】
export const AUTO_DOWNLOAD_ENABLE = false; 
export const FILE_SIZE_THRESHOLD = 10 * 1024 * 1024; // 10MB 自动下载阈值
export const FILE_CACHE_KEY = 'chat_file_cache_index'; // 本地文件缓存索引key
export const USER_INFO_STORAGE_KEY = 'chat_user_info'; // 用户信息存储key

// ==================== 内置API适配（兼容uni-app和纯H5） ====================
export const uniApiAdapter = {
  showToast: (options) => {
    if (typeof uni !== 'undefined') return uni.showToast(options);
    alert(options.title);
  },
  showModal: (options) => {
    if (typeof uni !== 'undefined') {
      return new Promise((resolve) => {
        uni.showModal({ ...options, success: (res) => resolve(res) });
      });
    }
    return new Promise((resolve) => {
      const result = confirm(options.content);
      resolve({ confirm: result, cancel: !result });
    });
  },
  getStorageSync: (key) => {
    if (typeof uni !== 'undefined') return uni.getStorageSync(key);
    try {
      const data = localStorage.getItem(key);
      return data ? JSON.parse(data) : null;
    } catch (e) {
      console.error('读取本地存储失败', e);
      return null;
    }
  },
  setStorageSync: (key, data) => {
    if (typeof uni !== 'undefined') return uni.setStorageSync(key, data);
    try {
      localStorage.setItem(key, JSON.stringify(data));
    } catch (e) {
      console.error('保存本地存储失败', e);
    }
  },
  removeStorageSync: (key) => {
    if (typeof uni !== 'undefined') return uni.removeStorageSync(key);
    localStorage.removeItem(key);
  }
};

// ==================== 【核心】文件缓存索引管理 ====================
/**
 * 获取完整的文件缓存索引
 * @returns {Array} 缓存索引列表
 */
const getFileCacheIndex = () => {
  try {
    const index = uniApiAdapter.getStorageSync(FILE_CACHE_KEY);
    return Array.isArray(index) ? index : [];
  } catch (e) {
    console.error('获取文件缓存索引失败', e);
    return [];
  }
};

/**
 * 保存文件记录到缓存索引
 * @param {Object} fileInfo 文件信息 { msgId, fileUrl, fileName, fileSize, localPath }
 */
const saveFileToCache = (fileInfo) => {
  if (!fileInfo?.msgId || !fileInfo?.localPath) {
    console.warn('保存文件缓存失败：缺少必要参数', fileInfo);
    return;
  }
  try {
    const index = getFileCacheIndex();
    // 去重：已存在的记录先删除（防止同一文件多条记录）
    const filterIndex = index.filter(item => 
      item.msgId !== fileInfo.msgId && item.fileUrl !== fileInfo.fileUrl
    );
    // 新记录加到最前面
    filterIndex.unshift({
      msgId: fileInfo.msgId,
      fileUrl: fileInfo.fileUrl || '',
      fileName: fileInfo.fileName || '未知文件',
      fileSize: fileInfo.fileSize || 0,
      localPath: fileInfo.localPath,
      downloadTime: Date.now()
    });
    // 限制缓存索引数量（防止无限增长，保留最近1000条）
    const finalIndex = filterIndex.slice(0, 1000);
    uniApiAdapter.setStorageSync(FILE_CACHE_KEY, finalIndex);
  } catch (e) {
    console.error('保存文件缓存索引失败', e);
  }
};

/**
 * 检查文件是否已下载，返回本地路径
 * @param {Object} fileInfo 文件信息 { msgId, fileUrl }
 * @returns {string|null} 本地路径，未下载返回null
 */
export const checkFileDownloaded = (fileInfo) => {
  if (!fileInfo?.msgId && !fileInfo?.fileUrl) return null;
  const index = getFileCacheIndex();
  const target = index.find(item => 
    (fileInfo.msgId && item.msgId === fileInfo.msgId) || 
    (fileInfo.fileUrl && item.fileUrl === fileInfo.fileUrl)
  );
  return target?.localPath || null;
};

/**
 * 批量初始化消息的已下载状态
 * @param {Array} msgList 消息列表
 * @returns {Array} 补全了local_file_url的消息列表
 */
export const initMsgDownloadStatus = (msgList) => {
  if (!Array.isArray(msgList) || msgList.length === 0) return msgList;
  return msgList.map(msg => {
    // 只处理文件类消息 (图片=2, 视频=3, 音频=4, 文件=5)
    if ([2, 3, 4, 5].includes(msg.message_type) && msg.file_url) {
      const localPath = checkFileDownloaded({ msgId: msg.id, fileUrl: msg.file_url });
      if (localPath) {
        return {
          ...msg,
          local_file_url: localPath,
          is_downloading: false,
          download_progress: 100
        };
      }
    }
    return msg;
  });
};

// ==================== 下载核心逻辑 ====================
/**
 * 生成本地存储文件名
 */
const generateLocalFileName = (fileName, msgId) => {
  const suffix = fileName.lastIndexOf('.') > -1 ? fileName.substring(fileName.lastIndexOf('.')) : '.bin';
  const pureName = fileName.substring(0, fileName.lastIndexOf('.') > -1 ? fileName.lastIndexOf('.') : fileName.length);
  return `chat_files/${msgId}_${pureName}${suffix}`;
};

/**
 * 执行文件下载
 * @param {string} url 远程文件地址
 * @param {string} fileName 文件名
 * @param {string|number} msgId 消息ID
 * @param {Function} progressCallback 进度回调 (progress) => {}
 * @param {Boolean} autoTrigger 是否自动触发（自动触发不弹浏览器下载）
 * @returns {Promise<string>} 本地缓存路径
 */
export const downloadFile = async (url, fileName, msgId, progressCallback, autoTrigger = false) => {
  if (!url) throw new Error('下载链接为空');
  if (!msgId) throw new Error('消息ID不能为空');

  // 【关键拦截】下载前再次检查是否已下载
  const existLocalPath = checkFileDownloaded({ msgId, fileUrl: url });
  if (existLocalPath) {
    progressCallback && progressCallback(100);
    return existLocalPath;
  }

  // 图片特殊处理：缓存Blob URL
  if (fileName?.toLowerCase().match(/\.(jpg|jpeg|png|gif|webp|bmp)$/)) {
    try {
      const response = await fetch(url);
      if (!response.ok) throw new Error('图片资源请求失败');
      const blob = await response.blob();
      const localUrl = URL.createObjectURL(blob);
      const localPath = generateLocalFileName(fileName, msgId);
      
      // 保存到缓存索引
      saveFileToCache({
        msgId,
        fileUrl: url,
        fileName,
        fileSize: blob.size,
        localPath: localUrl
      });
      return localUrl;
    } catch (e) {
      throw new Error(`图片缓存失败：${e.message}`);
    }
  }

  // 普通文件下载
  try {
    const response = await fetch(url);
    if (!response.ok) throw new Error('文件下载请求失败');
    
    const contentLength = response.headers.get('content-length');
    const totalSize = contentLength ? parseInt(contentLength) : 0;
    const reader = response.body.getReader();
    const chunks = [];
    let loadedSize = 0;

    // 读取流并更新进度
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      chunks.push(value);
      loadedSize += value.length;
      const progress = totalSize > 0 ? Math.floor((loadedSize / totalSize) * 100) : 0;
      progressCallback && progressCallback(progress);
    }

    // 创建Blob
    const blob = new Blob(chunks);
    const blobUrl = URL.createObjectURL(blob);
    const localPath = generateLocalFileName(fileName, msgId);

    // 【关键修复2：只有用户手动点击才触发浏览器下载，自动触发不下载】
    if (!autoTrigger) {
      const a = document.createElement('a');
      a.href = blobUrl;
      a.download = fileName;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(a.href);
    }

    // 保存下载记录到索引
    saveFileToCache({
      msgId,
      fileUrl: url,
      fileName,
      fileSize: totalSize || loadedSize,
      localPath: blobUrl
    });
    return blobUrl;
  } catch (e) {
    throw new Error(`文件下载失败：${e.message}`);
  }
};

// ==================== 对外暴露的消息下载处理方法 ====================
/**
 * 处理消息文件下载（点击文件气泡时调用）
 * @param {Object} msg 消息对象
 * @param {Function} updateMsgCallback 更新消息状态的回调 (updatedMsg) => {}
 * @param {Boolean} autoTrigger 是否自动触发
 */
export const handleMsgDownload = async (msg, updateMsgCallback, autoTrigger = false) => {
  if (msg.is_downloading) {
    // 自动触发不弹toast，避免刷屏
    if (!autoTrigger) uniApiAdapter.showToast({ title: '正在下载中，请稍候...', icon: 'none' });
    return;
  }
  
  // 【关键】先检查是否已下载
  const existLocalPath = checkFileDownloaded({ msgId: msg.id, fileUrl: msg.file_url });
  if (existLocalPath) {
    updateMsgCallback({ ...msg, local_file_url: existLocalPath });
    if (!autoTrigger) uniApiAdapter.showToast({ title: '文件已在本地', icon: 'success' });
    return;
  }

  if (!msg.file_url) {
    if (!autoTrigger) uniApiAdapter.showToast({ title: '文件链接无效', icon: 'none' });
    return;
  }

  // 大文件二次确认（仅手动点击触发）
  const isLargeFile = msg.file_size > FILE_SIZE_THRESHOLD;
  if (isLargeFile && !autoTrigger) {
    const res = await uniApiAdapter.showModal({
      title: '大文件下载确认',
      content: `文件大小为 ${(msg.file_size / 1024 / 1024).toFixed(1)} MB，是否确认下载？`,
      confirmText: '确认下载',
      cancelText: '取消'
    });
    if (!res.confirm) return;
  }

  // 更新状态为下载中
  updateMsgCallback({ ...msg, is_downloading: true, download_progress: 0 });

  try {
    const localPath = await downloadFile(
      msg.file_url,
      msg.file_name || `未知文件_${Date.now()}`,
      msg.id,
      (progress) => updateMsgCallback({ ...msg, download_progress: progress }),
      autoTrigger
    );

    // 下载完成，更新最终状态
    updateMsgCallback({
      ...msg,
      is_downloading: false,
      download_progress: 100,
      local_file_url: localPath
    });
    if (!autoTrigger) {
      uniApiAdapter.showToast({ 
        title: isLargeFile ? '大文件下载成功' : '文件下载成功', 
        icon: 'success' 
      });
    }
  } catch (e) {
    // 下载失败，重置状态
    updateMsgCallback({ ...msg, is_downloading: false, download_progress: 0 });
    if (!autoTrigger) uniApiAdapter.showToast({ title: e.message || '下载失败', icon: 'none' });
  }
};

// ==================== 用户信息持久化工具 ====================
/**
 * 从本地存储读取用户信息
 * @returns {Object|null} 用户信息对象
 */
export const getUserInfoFromStorage = () => {
  try {
    const userInfo = uniApiAdapter.getStorageSync(USER_INFO_STORAGE_KEY);
    return userInfo && typeof userInfo === 'object' ? userInfo : null;
  } catch (e) {
    console.error('读取用户信息失败', e);
    return null;
  }
};

/**
 * 保存用户信息到本地存储
 * @param {Object} userInfo 用户信息对象
 */
export const saveUserInfoToStorage = (userInfo) => {
  if (!userInfo || typeof userInfo !== 'object') {
    console.warn('保存用户信息失败：数据格式不正确', userInfo);
    return;
  }
  try {
    uniApiAdapter.setStorageSync(USER_INFO_STORAGE_KEY, userInfo);
  } catch (e) {
    console.error('保存用户信息失败', e);
  }
};