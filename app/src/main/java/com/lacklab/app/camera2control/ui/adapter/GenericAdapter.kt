package com.lacklab.app.camera2control.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import timber.log.Timber

typealias BindCallback<T> = (view: View, data: T, position: Int) -> Unit

class GenericAdapter<T>(
    private val dataset: List<T>,
    private val itemLayoutId: Int,
    private val onBind: BindCallback<T>
) : RecyclerView.Adapter<GenericAdapter.GenericViewHolder>(){

    class GenericViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericViewHolder {
        Timber.d("create holder")
        return GenericViewHolder(LayoutInflater.from(parent.context)
            .inflate(itemLayoutId, parent, false))
    }


    override fun getItemCount() = dataset.size

    override fun onBindViewHolder(holder: GenericViewHolder, position: Int) {
        if(position < 0 || position > dataset.size) return
        onBind(holder.view, dataset[position], position)
    }

}