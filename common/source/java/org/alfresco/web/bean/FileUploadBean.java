/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.web.bean;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.io.FilenameUtils;

import ee.webmedia.alfresco.document.file.web.AddFileDialog;

/**
 * Bean to hold the results of a file upload
 * 
 * @author gavinc
 */
public final class FileUploadBean implements Serializable
{

   private static final long serialVersionUID = 7667383955924957544L;
   
   public static final String FILE_UPLOAD_BEAN_NAME = "alfresco.UploadBean";
   private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(FileUploadBean.class);

   public static String getKey(final String id)
   {
    return ((id == null || id.length() == 0) 
        ? FILE_UPLOAD_BEAN_NAME 
        : FILE_UPLOAD_BEAN_NAME + "-" + id);
   }
   
   private List<File> file;
   private List<String> fileName;
   private List<String> fileNameWithoutExtension;
   private List<Boolean> associatedWithMetaData;
   private List<String> filePath;
   private List<String> contentType;
   private List<Long> orderNumbers;
   private List<NodeRef> taskRefs;
   private boolean multiple = false;

   private boolean problematicFile = false;

   public void setProblematicFile(boolean problematicFile) {
       this.problematicFile = problematicFile;
   }

   public boolean isProblematicFile() {
       return problematicFile;
   }

   /**
    * @return Returns the file
    */
   public File getFile()
   {
      if(!multiple)
      return file.get(0);
      
      String msg = "FileUploadBean is in multiple files mode,fetching single file is not supported!";
      log.debug(msg);
      throw new RuntimeException(msg);
   }
   
   /**
    * @param file The file to set
    */
   public void setFile(File file)
   {
        setProblematicFile(false);
        if(this.file == null) 
            this.file = new ArrayList<File>();
        
        if (!multiple) {
            this.file.add(0, file);
        } else {
            this.file.add(file);
        }
   }
   
   public List<File> getFiles() {
       return file;
   }

   public void setFile(List<File> file) {
       setProblematicFile(false);
       this.file = file;
   }

   /**
    * @return Returns the name of the file uploaded
    */
   public String getFileName()
   {
        if (!multiple)
            return fileName.get(0);

        String msg = "FileUploadBean is in multiple files mode, fetching single filename is not supported!";
        log.debug(msg);
        throw new RuntimeException(msg);
   }

   /**
    * @param fileName The name of the uploaded file
    */
   public void setFileName(String fileName)
   {
       if(this.fileName == null) 
           this.fileName = new ArrayList<String>(10);
           
       if(this.fileNameWithoutExtension == null)
           this.fileNameWithoutExtension = new ArrayList<String>(10);
       
       if (associatedWithMetaData == null) {
           associatedWithMetaData = new ArrayList<Boolean>(10);
       }

        String filenameWithoutExtension = FilenameUtils.removeExtension(fileName);
        boolean associateWithMetadata = AddFileDialog.BOUND_METADATA_EXTENSIONS.contains(FilenameUtils.getExtension(fileName));
        if (!multiple) {
            this.fileName.add(0, fileName);
            this.fileNameWithoutExtension.add(0, filenameWithoutExtension);
            associatedWithMetaData.add(0, associateWithMetadata);
        } else {
            this.fileName.add(fileName);
            this.fileNameWithoutExtension.add(filenameWithoutExtension);
            associatedWithMetaData.add(associateWithMetadata);
        }
   }

    public List<String> getFileNames() {
        return fileName;
    }

    public void setFileName(List<String> fileName) {
        this.fileName = fileName;
    }

    public List<String> getFileNameWithoutExtension() {
        return fileNameWithoutExtension;
    }

    public void setFileNameWithoutExtension(List<String> fileNameWithoutExtension) {
        this.fileNameWithoutExtension = fileNameWithoutExtension;
    }
    
    public List<Boolean> getAssociatedWithMetaData() {
        return associatedWithMetaData;
    }

    public void setAssociatedWithMetaData(List<Boolean> associatedWithMetaData) {
        this.associatedWithMetaData = associatedWithMetaData;
    }

    public List<Long> getOrderNumbers() {
        return orderNumbers;
    }

    public void setOrderNumbers(List<Long> orderNumbers) {
        this.orderNumbers = orderNumbers;
    }

/**
    * @return Returns the path of the file uploaded
    */
   public String getFilePath()
   {
        if (!multiple)
            return filePath.get(0);

        String msg = "FileUploadBean is in multiple files mode, fetching single file path is not supported!";
        log.debug(msg);
        throw new RuntimeException(msg);
   }

   /**
    * @param filePath The file path of the uploaded file
    */
   public void setFilePath(String filePath)
   {
        if(this.filePath == null) 
            this.filePath = new ArrayList<String>(10);

        if (!multiple) {
            this.filePath.add(0, filePath);
        } else {
            this.filePath.add(filePath);
        }
  }
   
   public List<String> getFilePaths() {
       return filePath;
   }

   public void setFilePath(List<String> filePath) {
       this.filePath = filePath;
   }

   /**
    * @return Returns the content type of the file uploaded
    */
   public String getContentType() {
        if (!multiple) {
            return contentType.get(0);
        }

        String msg = "FileUploadBean is in multiple files mode, fetching single content type is not supported!";
        log.debug(msg);
        throw new RuntimeException(msg);
   }
    
   /**
    * @param contentType The content type of the uploaded file
    */
   public void setContentType(String contentType) {
       
       if(this.contentType == null) 
           this.contentType = new ArrayList<String>(10);
       
       if (!multiple) {
           this.contentType.add(0, contentType);
       } else {
           this.contentType.add(contentType);
       }
   }
   
   public List<String> getContentTypes() {
       return contentType;
   }

   public void setContentType(List<String> contentType) {
       this.contentType = contentType;
   }

   /**
    * @return Returns true if multiple file mode should be used 
    */
   public boolean isMultiple() {
       return multiple;
   }
    
   /**
    * @param multiple set whether multiple file mode should be used
    */
   public void setMultiple(boolean multiple) {
       this.multiple = multiple;
   }

    public void removeFile(int index) {
        getFiles().remove(index);
        getFileNames().remove(index);
        getFileNameWithoutExtension().remove(index);
        getContentTypes().remove(index);
        getFilePaths().remove(index);
        getAssociatedWithMetaData().remove(index);
        if (getOrderNumbers() != null && getOrderNumbers().size() > index) {
            getOrderNumbers().remove(index);
        }
        if (getTaskRefs() != null && getTaskRefs().size() > index) {
            getTaskRefs().remove(index);
        }
    }

    public void removeFiles(List<Integer> fileIndexes) {
        Collections.sort(fileIndexes, Collections.reverseOrder());
        for (int index : fileIndexes) {
            removeFile(index);
        }
    }

    public boolean removeFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        List<File> files = getFiles();
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            if (fileName.equals(file.getName())) {
                removeFile(i);
                return true;
            }
        }
        return false;
    }

    public List<NodeRef> getTaskRefs() {
        return taskRefs;
    }

    public void setTaskRef(NodeRef taskRef) {
        if (taskRefs == null) {
            taskRefs = new ArrayList<>();
        }
        taskRefs.add(taskRef);
    }
}
