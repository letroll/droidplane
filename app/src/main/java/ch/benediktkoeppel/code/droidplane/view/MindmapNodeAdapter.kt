package ch.benediktkoeppel.code.droidplane.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter

/**
 * The MindmapNodeAdapter is the data provider for the NodeColumn (respectively its ListView).
 */
internal class MindmapNodeAdapter(context: Context, textViewResourceId: Int, private val mindmapNodeLayouts: List<MindmapNodeLayout>?) : ArrayAdapter<MindmapNodeLayout?>(context, textViewResourceId, mindmapNodeLayouts?: emptyList()) {

    /**
     * getView is responsible to return a view for each individual element in the ListView
     * @param int position: the position in the mindmapNodes array, for which we need to generate a view
     * @param View convertView: the view we should recycle
     * @param ViewGroup parent: not sure, is this the NodeColumn for which the Adapter is generating views?
     * @see android.widget.ArrayAdapter#getView(int, android.view.View, android.view.ViewGroup)
     */
    @SuppressLint("InlinedApi") override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // when convertView != null, we should take the convertView and update it appropriately. Android is
        // optimizing the performance and thus recycling GUI elements. However, we don't want to recycle anything,
        // because these are genuine Mindmap nodes. Recycling the view here would show one node twice in the tree,
        // while leaving out the actual node we should display.

        val view = mindmapNodeLayouts?.get(position)

        // tell the node to refresh it's view
        view?.refreshView()
        //TODO check if used
        return view!!
    }
}
