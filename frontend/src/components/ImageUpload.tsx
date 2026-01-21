/**
 * ═══════════════════════════════════════════════════════════════════════
 * IMAGE UPLOAD COMPONENT - Process Diagram to BPMN Conversion
 * ═══════════════════════════════════════════════════════════════════════
 * 
 * Allows users to upload images of process diagrams:
 * - Hand-drawn flowcharts
 * - Whiteboard photos
 * - Screenshots of existing diagrams
 * - Any visual process representation
 * 
 * AI Vision (GPT-4o) analyzes the image and generates BPMN.
 * 
 * ═══════════════════════════════════════════════════════════════════════
 */

import { useState, useRef } from 'react'
import axios from 'axios'
import './ImageUpload.css'

interface ImageUploadProps {
  onProcessCreated: (processId: string) => void
}

const ImageUpload = ({ onProcessCreated }: ImageUploadProps) => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [processName, setProcessName] = useState('')
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return

    // Validate file type
    if (!file.type.startsWith('image/')) {
      setError('Please select an image file (JPEG, PNG, etc.)')
      return
    }

    // Validate file size (max 10MB)
    if (file.size > 10 * 1024 * 1024) {
      setError('Image too large. Maximum 10MB allowed.')
      return
    }

    setSelectedFile(file)
    setError(null)

    // Create preview
    const reader = new FileReader()
    reader.onloadend = () => {
      setPreviewUrl(reader.result as string)
    }
    reader.readAsDataURL(file)
  }

  const handleUpload = async () => {
    if (!selectedFile) return

    setUploading(true)
    setError(null)

    try {
      // Create FormData for multipart upload
      const formData = new FormData()
      formData.append('image', selectedFile)
      if (processName.trim()) {
        formData.append('name', processName.trim())
      }

      // Upload to backend
      const response = await axios.post(
        'http://localhost:8080/api/process/from-image',
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        }
      )

      const { processId } = response.data

      console.log('[ImageUpload] Process created from image:', processId)

      // Notify parent component
      onProcessCreated(processId)

      // Reset form
      setSelectedFile(null)
      setPreviewUrl(null)
      setProcessName('')
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
    } catch (err: any) {
      console.error('[ImageUpload] Upload failed:', err)
      setError(
        err.response?.data?.message || 
        'Failed to create process from image. Please try again.'
      )
    } finally {
      setUploading(false)
    }
  }

  const handleClear = () => {
    setSelectedFile(null)
    setPreviewUrl(null)
    setProcessName('')
    setError(null)
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  return (
    <div className="image-upload-container">
      <h3>📸 Upload Process Diagram</h3>
      <p className="upload-description">
        Upload a photo or screenshot of your process diagram.
        AI will analyze it and generate a BPMN model.
      </p>

      <div className="upload-form">
        {/* File Input */}
        <div className="file-input-section">
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            onChange={handleFileSelect}
            disabled={uploading}
            className="file-input"
            id="image-file-input"
          />
          <label htmlFor="image-file-input" className="file-input-label">
            {selectedFile ? '📎 Change Image' : '📁 Select Image'}
          </label>
        </div>

        {/* Preview */}
        {previewUrl && (
          <div className="image-preview-section">
            <img src={previewUrl} alt="Preview" className="image-preview" />
            <div className="preview-info">
              <p><strong>File:</strong> {selectedFile?.name}</p>
              <p><strong>Size:</strong> {((selectedFile?.size || 0) / 1024).toFixed(2)} KB</p>
            </div>
          </div>
        )}

        {/* Process Name Input */}
        {selectedFile && (
          <div className="name-input-section">
            <label htmlFor="process-name">Process Name (Optional):</label>
            <input
              id="process-name"
              type="text"
              value={processName}
              onChange={(e) => setProcessName(e.target.value)}
              placeholder="Leave empty to extract from image"
              disabled={uploading}
              className="process-name-input"
            />
          </div>
        )}

        {/* Error Display */}
        {error && (
          <div className="upload-error">
            ⚠️ {error}
          </div>
        )}

        {/* Action Buttons */}
        {selectedFile && (
          <div className="upload-actions">
            <button
              onClick={handleUpload}
              disabled={uploading}
              className="btn-upload"
            >
              {uploading ? '🔄 Analyzing Image...' : '🚀 Create Process'}
            </button>
            <button
              onClick={handleClear}
              disabled={uploading}
              className="btn-clear"
            >
              ✕ Clear
            </button>
          </div>
        )}
      </div>

      {/* Tips */}
      <div className="upload-tips">
        <h4>💡 Tips for Best Results:</h4>
        <ul>
          <li>Use clear, well-lit images</li>
          <li>Ensure text is readable</li>
          <li>Include all process steps and connections</li>
          <li>Supported formats: JPEG, PNG, GIF, WebP</li>
          <li>Maximum file size: 10MB</li>
        </ul>
      </div>
    </div>
  )
}

export default ImageUpload
