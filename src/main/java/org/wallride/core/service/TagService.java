package org.wallride.core.service;

import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.wallride.core.domain.Tag;
import org.wallride.core.repository.TagRepository;
import org.wallride.core.support.AuthorizedUser;

import javax.annotation.Resource;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.logging.Level;
import org.wallride.core.domain.Article;
import org.wallride.core.repository.ArticleRepository;
import org.wallride.web.controller.admin.tag.TagMergeForm;
import org.wallride.web.support.DomainObjectSavedModel;

@Service
@Transactional(rollbackFor = Exception.class)
public class TagService {

	private static Logger logger = LoggerFactory.getLogger(TagService.class);

	@Resource
	private TagRepository tagRepository;
	@Resource
	private ArticleRepository articleRepository;
	@Inject
	private MessageCodesResolver messageCodesResolver;
	@Inject
	private PlatformTransactionManager transactionManager;

	@CacheEvict(value = "articles", allEntries = true)
	public Tag createTag(TagCreateRequest request, AuthorizedUser authorizedUser) {
		Tag duplicate = tagRepository.findByName(request.getName(), request.getLanguage());
		if (duplicate != null) {
			throw new DuplicateNameException(request.getName());
		}

		Tag tag = new Tag();
		LocalDateTime now = LocalDateTime.now();

		tag.setName(request.getName());
		tag.setLanguage(request.getLanguage());

		tag.setCreatedAt(now);
		tag.setCreatedBy(authorizedUser.toString());
		tag.setUpdatedAt(now);
		tag.setUpdatedBy(authorizedUser.toString());
		return tagRepository.saveAndFlush(tag);
	}

	@CacheEvict(value = "articles", allEntries = true)
	public Tag updateTag(TagUpdateRequest request, AuthorizedUser authorizedUser) {
		Tag tag = tagRepository.findByIdForUpdate(request.getId(), request.getLanguage());
		LocalDateTime now = LocalDateTime.now();

		if (!ObjectUtils.nullSafeEquals(tag.getName(), request.getName())) {
			Tag duplicate = tagRepository.findByName(request.getName(), request.getLanguage());
			if (duplicate != null) {
				throw new DuplicateNameException(request.getName());
			}
		}

		tag.setName(request.getName());
		tag.setLanguage(request.getLanguage());

		tag.setUpdatedAt(now);
		tag.setUpdatedBy(authorizedUser.toString());

		return tagRepository.saveAndFlush(tag);
	}

	@CacheEvict(value = "articles", allEntries = true)
	public Tag mergeTags(TagMergeRequest request, AuthorizedUser authorizedUser) {
		// Get all articles that have tag for merging
		ArticleSearchRequest searchRequest = new ArticleSearchRequest()
				.withTagIds(request.getIds());
		Page<Article> articles = articleRepository.search(searchRequest);

		// Delete old tag after merging
		for (long id : request.getIds()) {
			tagRepository.delete(id);
		}

		// Create a new Tag
		TagCreateRequest createRequest  = new TagCreateRequest.Builder()
				.name(request.getName())
				.language(request.getLanguage())
				.build();
		Tag mergedTag = createTag(createRequest, authorizedUser);

		for (Article article : articles) {
			article.getTags().add(mergedTag);
			articleRepository.saveAndFlush(article);
		}

		return mergedTag;
	}

	@CacheEvict(value = "articles", allEntries = true)
	public Tag deleteTag(TagDeleteRequest request, BindingResult result) {
		Tag tag = tagRepository.findByIdForUpdate(request.getId(), request.getLanguage());
		tagRepository.delete(tag);
		return tag;
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	@CacheEvict(value = "articles", allEntries = true)
	public List<Tag> bulkDeleteTag(TagBulkDeleteRequest bulkDeleteRequest, final BindingResult result) {
		List<Tag> tags = new ArrayList<>();
		for (long id : bulkDeleteRequest.getIds()) {
			final TagDeleteRequest deleteRequest = new TagDeleteRequest.Builder()
					.id(id)
					.language(bulkDeleteRequest.getLanguage())
					.build();

			final BeanPropertyBindingResult r = new BeanPropertyBindingResult(deleteRequest, "request");
			r.setMessageCodesResolver(messageCodesResolver);

			TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
			transactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
			Tag tag = null;
			try {
				tag = transactionTemplate.execute(new TransactionCallback<Tag>() {
					public Tag doInTransaction(TransactionStatus status) {
						return deleteTag(deleteRequest, result);
					}
				});
				tags.add(tag);
			} catch (Exception e) {
				logger.debug("Errors: {}", r);
				result.addAllErrors(r);
			}
		}
		return tags;
	}

	public Tag readTagById(long id, String language) {
		return tagRepository.findById(id, language);
	}

	public Tag readTagByName(String name, String language) {
		return tagRepository.findByName(name, language);
	}

	public Page<Tag> readTags(TagSearchRequest request) {
		Pageable pageable = new PageRequest(0, 10);
		return readTags(request, pageable);
	}

	public Page<Tag> readTags(TagSearchRequest request, Pageable pageable) {
		return tagRepository.search(request, pageable);
	}
}
