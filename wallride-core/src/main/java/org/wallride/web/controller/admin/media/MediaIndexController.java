/*
 * Copyright 2014 Tagbangers, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wallride.web.controller.admin.media;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.wallride.core.domain.Media;
import org.wallride.core.service.MediaService;
import org.wallride.core.support.WallRideProperties;

import javax.inject.Inject;
import java.util.List;

@Controller
@RequestMapping("/{language}/media/index")
public class MediaIndexController {

	@Inject
	private MediaService mediaService;
	@Inject
	private WallRideProperties wallRideProperties;

	@RequestMapping
	public @ResponseBody MediaIndexModel[] index() {
		List<Media> medias = mediaService.getAllMedias();
		MediaIndexModel[] models = new MediaIndexModel[medias.size()];
		for (int i = 0; i < medias.size(); i++) {
			models[i] = new MediaIndexModel(medias.get(i), wallRideProperties);
		}
		return models;
	}
}