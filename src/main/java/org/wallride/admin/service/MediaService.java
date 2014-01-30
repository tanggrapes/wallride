package org.wallride.admin.service;

import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.wallride.core.domain.Media;
import org.wallride.core.domain.Setting;
import org.wallride.core.repository.MediaRepository;
import org.wallride.core.support.AmazonS3ResourceUtils;
import org.wallride.core.support.Settings;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

@Service @Lazy
@Transactional(rollbackFor=Exception.class)
public class MediaService {

	@Inject
	private MediaRepository mediaRepository;

	@Inject
	private ResourceLoader resourceLoader;

	@Inject
	private Settings settings;

	public Media createMedia(MultipartFile file) {
		Media media = new Media();
		media.setMimeType(file.getContentType());
		media.setOriginalName(file.getOriginalFilename());
		media = mediaRepository.saveAndFlush(media);

		try {
			Resource prefix = resourceLoader.getResource(settings.readSettingAsString(Setting.Key.MEDIA_PATH));
			Resource resource = prefix.createRelative(media.getId());
			AmazonS3ResourceUtils.writeMultipartFile(file, resource);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}

		return media;
	}

//	public Media[] createMedia(MultipartFile[] files) {
//		Media[] medias = new Media[files.length];
//		for (int i = 0; i < files.length; i++) {
//			medias[i] = createMedia(files[i]);
//		}
//		return medias;
//	}

	public List<Media> readAllMedias() {
		return mediaRepository.findAll(new Sort(new Sort.Order(Sort.Direction.DESC, "createdAt")));
	}

	public Media readMedia(String id) {
		return mediaRepository.findById(id);
	}
}
