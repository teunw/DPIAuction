from django.contrib import admin

# Register your models here.
from auction.models import Auction, Bid


class AuctionAdmin(admin.ModelAdmin):
    all_fields = ('id', 'itemName', 'creator')
    list_display = all_fields
    list_display_links = all_fields
    search_fields = all_fields


class BidAdmin(admin.ModelAdmin):
    all_fields = ('id', 'amount', 'auction', 'bidder')
    list_display = all_fields
    list_display_links = all_fields
    search_fields = all_fields


admin.site.register(Auction, AuctionAdmin)
admin.site.register(Bid, BidAdmin)
