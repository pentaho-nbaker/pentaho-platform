/*
 * This program is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU General Public License, version 2 as published by the Free Software 
 * Foundation.
 *
 * You should have received a copy of the GNU General Public License along with this 
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/gpl-2.0.html 
 * or from the Free Software Foundation, Inc., 
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package org.pentaho.platform.repository2.unified.webservices;

import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.repository2.unified.RepositoryFileSid;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.*;

/**
 * Converts {@code RepositoryFile} into JAXB-safe object and vice-versa.
 *
 * @author mlowery
 */
public class RepositoryFileAdapter extends XmlAdapter<RepositoryFileDto, RepositoryFile> {

  @Override
  public RepositoryFileDto marshal(final RepositoryFile v) {
    return toFileDto(v, null);
  }

  private static boolean include(String key, Set<String> set){
    return set == null || set.contains(key);
  }
  public static RepositoryFileDto toFileDto(final RepositoryFile v, Set<String> includeMemberSet) {
    if (v == null) {
      return null;
    }
    RepositoryFileDto f = new RepositoryFileDto();
    if(include("name", includeMemberSet)){
      f.name = v.getName();
    }
    if(include("path", includeMemberSet)){
      f.path = v.getPath();
    }
    if(include("hidden", includeMemberSet)){
      f.hidden = v.isHidden();
    }
    if(include("createDate", includeMemberSet)){
      f.createdDate = v.getCreatedDate();
    }
    if(include("creatorId", includeMemberSet)){
      f.creatorId = v.getCreatorId();
    }
    if(include("fileSize", includeMemberSet)){
      f.fileSize = v.getFileSize();
    }
    if(include("description", includeMemberSet)){
      f.description = v.getDescription();
    }
    if(include("folder", includeMemberSet)){
      f.folder = v.isFolder();
    }
    if(include("id", includeMemberSet)){
      if (v.getId() != null) {
        f.id = v.getId().toString();
      }
    }
    if(include("lastModifiedDate", includeMemberSet)){
      f.lastModifiedDate = v.getLastModifiedDate();
    }
    if(include("locale", includeMemberSet)){
      f.locale = v.getLocale();
    }
    if(include("originalParentFolderPath", includeMemberSet)){
      f.originalParentFolderPath = v.getOriginalParentFolderPath();
    }
    if(include("deletedDate", includeMemberSet)){
      f.deletedDate = v.getDeletedDate();
    }
    if(include("lockDate", includeMemberSet)){
      f.lockDate = v.getLockDate();
    }
    if(include("locked", includeMemberSet)){
      f.locked = v.isLocked();
    }
    if(include("lockMessage", includeMemberSet)){
      f.lockMessage = v.getLockMessage();
    }
    if(include("lockOwner", includeMemberSet)){
      f.lockOwner = v.getLockOwner();
    }
    if(include("title", includeMemberSet)){
      f.title = v.getTitle();
    }
    if(include("versioned", includeMemberSet)){
      f.versioned = v.isVersioned();
    }
    if(include("versionId", includeMemberSet)){
      if (v.getVersionId() != null) {
        f.versionId = v.getVersionId().toString();
      }
    }
    if(include("locales", includeMemberSet)){
      if (v.getLocalePropertiesMap() != null) {
        f.localePropertiesMapEntries = new ArrayList<LocaleMapDto>();
        for (Map.Entry<String, Properties> entry : v.getLocalePropertiesMap().entrySet()) {

          LocaleMapDto localeMapDto = new LocaleMapDto();
          List<StringKeyStringValueDto> valuesDto = new ArrayList<StringKeyStringValueDto>();

          Properties properties = entry.getValue();
          if(properties != null){
            for(String propertyName : properties.stringPropertyNames()){
              valuesDto.add(new StringKeyStringValueDto(propertyName, properties.getProperty(propertyName)));
            }
          }

          localeMapDto.setLocale(entry.getKey());
          localeMapDto.setProperties(valuesDto);

          f.localePropertiesMapEntries.add(localeMapDto);
        }
      }
    }

    return f;
  }

  @Override
  public RepositoryFile unmarshal(final RepositoryFileDto v) {
    return toFile(v);
  }

  public static RepositoryFile toFile(final RepositoryFileDto v) {
    if (v == null) {
      return null;
    }
    RepositoryFile.Builder builder = null;
    if (v.id != null) {
      builder = new RepositoryFile.Builder(v.id, v.name);
    } else {
      builder = new RepositoryFile.Builder(v.name);
    }
    RepositoryFileSid owner = null;
    if (v.ownerType != -1) {
      owner = new RepositoryFileSid(v.owner, RepositoryFileSid.Type.values()[v.ownerType]);
    } else {
      owner = null;
    }
    if (v.localePropertiesMapEntries != null) {
      for (LocaleMapDto localeMapDto : v.localePropertiesMapEntries) {

        String locale = localeMapDto.getLocale();
        Properties localeProperties = new Properties();

        if(localeMapDto.getProperties() != null){
          for(StringKeyStringValueDto keyValueDto: localeMapDto.getProperties()){
            localeProperties.put(keyValueDto.getKey(), keyValueDto.getValue());
          }
        }

        builder.localeProperties(locale, localeProperties);
      }
    }

    return builder.path(v.path).createdDate(v.createdDate).creatorId(v.creatorId).description(v.description).folder(v.folder).fileSize(v.fileSize)
        .lastModificationDate(v.lastModifiedDate).locale(v.locale).lockDate(v.lockDate).locked(v.locked).lockMessage(
            v.lockMessage).lockOwner(v.lockOwner).title(v.title).versioned(v.versioned).versionId(v.versionId)
        .originalParentFolderPath(v.originalParentFolderPath)
        .deletedDate(v.deletedDate).hidden(v.hidden).build();
  }

}
