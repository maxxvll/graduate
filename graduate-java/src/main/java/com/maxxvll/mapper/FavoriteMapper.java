package com.maxxvll.mapper;

import com.maxxvll.domain.Favorite;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author 20570
* @description 针对表【chat_favorite(用户收藏表)】的数据库操作Mapper
* @createDate 2026-03-30
* @Entity com.maxxvll.domain.Favorite
*/
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

}