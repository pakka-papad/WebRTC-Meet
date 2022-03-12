package com.example.webrtcmeet.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.webrtcmeet.R
import com.example.webrtcmeet.models.User
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class CallListAdapter(options: FirestoreRecyclerOptions<User>, private val listener: ICallListAdapter): FirestoreRecyclerAdapter<User, CallListAdapter.CallViewHolder>(options) {
    class CallViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val profilePicture: ImageView = itemView.findViewById(R.id.profile_picture)
        val userName: TextView = itemView.findViewById(R.id.user_name)
        val connectBtn: Button = itemView.findViewById(R.id.connect_btn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallViewHolder {
        val viewHolder = CallViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.call_card,parent,false))
        viewHolder.connectBtn.setOnClickListener {
            listener.onConnectClicked(snapshots.getSnapshot(viewHolder.adapterPosition).id)
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: CallViewHolder, position: Int, model: User) {
        holder.userName.text = model.displayName
        holder.profilePicture.load(model.photoUrl)
    }
}

interface ICallListAdapter {
    fun onConnectClicked(uid: String)
}
