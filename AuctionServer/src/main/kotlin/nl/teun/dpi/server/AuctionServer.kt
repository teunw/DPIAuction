package nl.teun.dpi.server

import com.google.inject.Guice
import nl.teun.dpi.common.data.Auction
import nl.teun.dpi.common.data.Bid
import nl.teun.dpi.common.data.User
import nl.teun.dpi.common.data.notifications.NewBidNotification
import nl.teun.dpi.server.builder.CommunicationSubscriber
import nl.teun.dpi.server.communication.messaging.KBus
import nl.teun.dpi.server.communication.messaging.KBusRequestReply
import nl.teun.dpi.server.communication.rest.AuctionRestClient
import nl.teun.dpi.common.data.replies.AuctionReply
import nl.teun.dpi.common.data.replies.NewAuctionReply
import nl.teun.dpi.common.data.requests.*
import nl.teun.dpi.common.rest.AuthenticationHandler
import nl.teun.dpi.server.services.AuctionModule
import nl.teun.dpi.common.toJson

fun main(args: Array<String>) {
    val injector = Guice.createInjector(AuctionModule())
    val auctionRest = injector.getInstance(AuctionRestClient::class.java)

    CommunicationSubscriber("*")
            .subscribeAdvanced<Any> { it, envelope ->
                println("Message was sent to the queue")
                println("${envelope.routingKey}: ${it.toJson()}")
            }

    KBusRequestReply().setupReplier<AuctionRequest, AuctionReply> {
        AuctionReply(auctionRest.getAuctions())
    }

    KBusRequestReply().setupReplier<NewAuctionRequest, NewAuctionReply> {
        try {
            val auction = auctionRest.addAuction(Auction(itemName = it.itemName, creator = auctionRest.getUserWithToken(it.token).toUser()))
            NewAuctionReply(true, updatedAuction = auction)
        } catch (e:Exception) {
            NewAuctionReply(false, reason = "User not found!")
        }
    }

    KBus().subscribe<AuctionDeleteRequest> {
        auctionRest.deleteAuction(it.auctionId)
        println("Removed auction #${it.auctionId}")
    }

    KBus().subscribe<NewBidRequest> {
        println("Received new bid: " + it.toJson())
        val newBid = it.newBid
        val auction = auctionRest.getAuctions().find { it.id == newBid.auction?.id }
                ?: throw Exception("Auction not found")

        val nAuction = auction.copy(bids = mutableListOf())
        val newBidCopy = newBid.copy(auction = nAuction)
        auctionRest.addBid(newBidCopy)
        auction.bids.add(newBidCopy)

        println("Added new bid ${newBid.toJson()}")

        val notification = NewBidNotification(newBidCopy)
        KBus().sendMessage(notification)
        println("Sent notification")
    }
}