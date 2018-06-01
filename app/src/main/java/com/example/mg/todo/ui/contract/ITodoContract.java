package com.example.mg.todo.ui.contract;

import com.example.mg.todo.data.model.NoteModel;

import java.util.List;

public interface ITodoContract {
    interface IView {
    }

    interface IPresenter {

        boolean onItemLongClick(int position);

        void onItemClick(int position);

        void onNoteClick(NoteModel note, int position);

        void onNoteDoneClick(NoteModel newNote, int mUpdated);

        void updateViewData(List<NoteModel> newData);

        void initMainRecyclerData();
    }
}

