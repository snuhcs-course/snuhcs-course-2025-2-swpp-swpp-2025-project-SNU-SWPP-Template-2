from django.contrib import admin
from .models import Post, PostLike, Comment, CommentLike, BookClub, BookClubMembership

admin.site.register(Post)
admin.site.register(PostLike)
admin.site.register(Comment)
admin.site.register(CommentLike)
admin.site.register(BookClub)
admin.site.register(BookClubMembership)
