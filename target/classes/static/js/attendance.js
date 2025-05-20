
// ヘッダーメニュー画面
function toggleMenu() {
	const menu = document.getElementById("dropdownMenu");
	menu.style.display = (menu.style.display === "block") ? "none" : "block";
}

// 画面外をクリックしたらメニューを閉じる
window.addEventListener('click', function(e) {
	const menu = document.getElementById("dropdownMenu");
	const toggle = document.querySelector('.menu-toggle');
	if (!menu.contains(e.target) && !toggle.contains(e.target)) {
		menu.style.display = "none";
	}
});

// 現在時刻を表示
document.addEventListener('DOMContentLoaded', function () {
  function updateDateTime() {
    const now = new Date();

    const weekdays = ['日', '月', '火', '水', '木', '金', '土'];
    const dayOfWeek = weekdays[now.getDay()];

    const dateStr = now.getFullYear() + '-' +
      String(now.getMonth() + 1).padStart(2, '0') + '-' +
      String(now.getDate()).padStart(2, '0') + '（' + dayOfWeek + '）';

    const timeStr = now.toLocaleTimeString('ja-JP', { hour12: false });

    document.getElementById('today-date').textContent = dateStr;
    document.getElementById('current-time').textContent = timeStr;
  }

  updateDateTime();
  setInterval(updateDateTime, 1000);
});

//$(document).ready(function() {
//    const token = $("meta[name='_csrf']").attr("content");
//    const header = $("meta[name='_csrf_header']").attr("content");
//
//    // Ajaxのすべてのリクエストに CSRF トークンを設定
//    $(document).ajaxSend(function(e, xhr, options) {
//        xhr.setRequestHeader(header, token);
//    });
//});
//
//$(document).ready(function() {
//    $('.edit-button').click(function() {
//        const date = $(this).data('date');
//        const clockType = $(this).data('type');
//
//        $.ajax({
//            url: `/attendance/getRecord`,
//            method: 'GET',
//            data: { date: date, clockType: clockType },
//            success: function(data) {
//                $('#editDate').val(data.workDate);
//                $('#editClockType').val(data.clockType);
//                $('#editClockIn').val(data.clockIn);
//                $('#editClockOut').val(data.clockOut);
//                $('#editType').val(data.attendanceType);
//                $('#editForm').show();
//            },
//            error: function() {
//                alert('データの取得に失敗しました。');
//            }
//        });
//    });
//});
//
//function submitEdit() {
//    const payload = {
//        workDate: $('#editDate').val(),
//        clockType: $('#editClockType').val(),
//        clockIn: $('#editClockIn').val(),
//        clockOut: $('#editClockOut').val(),
//        attendanceType: $('#editType').val()
//    };
//
//    $.ajax({
//        url: '/attendance/updateRecord',
//        method: 'POST',
//        contentType: 'application/json',
//        data: JSON.stringify(payload),
//        success: function() {
//            alert('更新成功');
//            location.reload();
//        },
//        error: function() {
//            alert('更新失敗');
//        }
//    });
//}