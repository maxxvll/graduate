export const downloadRemoteFileToLocalPath = async (url) => {
  const targetUrl = String(url || '').trim()
  if (!targetUrl) {
    throw new Error('download url is required')
  }

  const result = await uni.downloadFile({ url: targetUrl })
  if (Number(result?.statusCode || 0) !== 200 || !result?.tempFilePath) {
    throw new Error(`download failed: ${result?.statusCode || 'unknown status'}`)
  }

  let localPath = result.tempFilePath
  if (typeof uni.saveFile === 'function') {
    try {
      const saveResult = await uni.saveFile({
        tempFilePath: result.tempFilePath,
      })
      localPath = saveResult?.savedFilePath || localPath
    } catch (error) {
      console.warn('[file-runtime] persist temp file failed, fallback to temp path', error)
    }
  }

  return {
    tempFilePath: result.tempFilePath,
    localPath,
  }
}
