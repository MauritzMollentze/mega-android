package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.data.mapper.FavouriteInfoMapper
import mega.privacy.android.app.data.mapper.toFavouriteInfo
import mega.privacy.android.app.data.mapper.UserUpdateMapper
import mega.privacy.android.app.data.mapper.mapMegaUserListToUserUpdate
import mega.privacy.android.app.presentation.mapper.FavouriteMapper
import mega.privacy.android.app.presentation.mapper.toFavourite

/**
 * Module for providing mapper dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
class MapperModule {

    @Provides
    fun provideUserUpdateMapper(): UserUpdateMapper = ::mapMegaUserListToUserUpdate

    @Provides
    fun provideFavouriteInfoMapper(): FavouriteInfoMapper = ::toFavouriteInfo

    @Provides
    fun provideFavouriteMapper(): FavouriteMapper = ::toFavourite
}